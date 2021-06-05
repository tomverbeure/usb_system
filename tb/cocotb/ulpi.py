

from collections import namedtuple
from enum import IntEnum, Flag, auto

import logging

import cocotb
from cocotb.triggers import RisingEdge, Timer, First, Event
from cocotb.queue import Queue

import usb
from usb_constants import *

class RegisterAddr(IntEnum):
    VENDOR_ID_LO                    = 0x00
    VENDOR_ID_HI                    = 0x01
    PRODUCT_ID_LO                   = 0x02
    PRODUCT_ID_HI                   = 0x03
    FUNC_CTRL                       = 0x04
    FUNC_CTRL_SET                   = 0x05
    FUNC_CTRL_CLR                   = 0x06
    IFC_CTRL                        = 0x07
    IFC_CTRL_SET                    = 0x08
    IFC_CTRL_CLR                    = 0x09
    OTG_CTRL                        = 0x0a
    OTG_CTRL_SET                    = 0x0b
    OTG_CTRL_CLR                    = 0x0c
    USB_INT_EN_RISE                 = 0x0d
    USB_INT_EN_RISE_SET             = 0x0e
    USB_INT_EN_RISE_CLR             = 0x0f
    USB_INT_EN_FALL                 = 0x10
    USB_INT_EN_FALL_SET             = 0x11
    USB_INT_EN_FALL_CLR             = 0x12
    USB_INT_STS                     = 0x13
    USB_INT_LATCH                   = 0x14
    DEBUG                           = 0x15
    SCRATCH_REG                     = 0x16
    SCRATCH_REG_SET                 = 0x17
    SCRATCH_REG_CLR                 = 0x18

class RegisterAction(Flag):
    RD          = auto()
    WR          = auto()
    SET         = auto()
    CLR         = auto()

class RegisterField:

    def __init__(self, addr, actions, reset, msb, lsb):
        pass

UlpiSignals = namedtuple("UlpiSignals", ['data2phy', 'data2link', 'stp', 'direction', 'nxt'])

#UlpiState   = namedtuple("UlpiState", [
#                    'vendor_id', 
#                    'product_id', 
#                    'xcvr_select', 'term_select', 'op_mode', 'reset', 'suspend',
#
#                    'fs_ls_serial_mode_6pin', 'fs_ls_serial_mode_3pin', 'carkit_mode', 'clock_suspend_m', 'auto_resume', 
#                    'indicator_complement', 'indicator_pass_thru', 'interface_protect_disable',
#
#                    'id_pullup', 'dp_pulldown', 'dm_pulldown', 'dischrg_vbus', 'chrg_vbus', 'drv_vbus', 
#                    'drv_vbus_external', 'use_external_vbus_indicator',
#
#                    'hostdisconnect_rise', 'vbus_valid_rise', 'sess_valid_rise', 'sess_end_rise', 'id_gnd_rise',
#                    'hostdisconnect_fall', 'vbus_valid_fall', 'sess_valid_fall', 'sess_end_fall', 'id_gnd_fall',
#                    'hostdisconnect', 'vbus_valid', 'sess_valid', 'sess_end', 'id_gnd',
#                    'hostdisconnect_latch', 'vbus_valid_latch', 'sess_valid_latch', 'sess_end_latch', 'id_gnd_latch',
#                    'debug',
#                    'scratch'
#                    ])

class UlpiPhy:

    def __init__(self, dut, clock, signals, **kwargs):

        self.dut        = dut
        self.clock      = clock
        self.signals    = signals

        self.cur_ls             = '_'
        self.cur_rx_active      = 0
        self.cur_rx_valid       = 0
        self.cur_rx_error       = 0
        self.cur_rx_data        = 0
        self.cur_rxcmd          = 0

        self.prev_ls            = '_'
        self.prev_rx_active     = 0
        self.prev_rx_valid      = 0
        self.prev_rx_error      = 0
        self.prev_rxcmd         = 0

        self.recalc_rxcmd()
        self.rxcmd_stale        = True

        self.rx_wire_event_queue    = Queue()

        if 'vendor_id' in kwargs.keys():
            self.vendor_id   = kwargs['vendor_id']
        else:
            self.vendor_id   = 0xbeef;

        if 'product_id' in kwargs.keys():
            self.product_id   = kwargs['product_id']
        else:
            self.product_id   = 0xcafe;

        self.id_pullup                    = 0
        self.dp_pulldown                  = 1
        self.dm_pulldown                  = 1
        self.dischrg_vbus                 = 0
        self.chrg_vbus                    = 0
        self.drv_vbus                     = 0
        self.drv_vbus_external            = 0
        self.use_external_vbus_indicator  = 0

        super().__init__()

        if self.signals.direction is not None:
            self.signals.direction.setimmediatevalue(0)

        if self.signals.nxt is not None:
            self.signals.nxt.setimmediatevalue(0)

        if self.signals.data2link is not None:
            self.signals.data2link.setimmediatevalue(0)

        self._run_cr            = None
#        self._rxcmd_update_cr   = None
        self._restart()

    def _restart(self):

        if self._run_cr is not None:
            self._run_cr.kill()
        self._run_cr = cocotb.fork(self._run())

#        if self._rxcmd_update_cr is not None:
#            self._rxcmd_update_cr.kill() 
#        self._rxcmd_update_cr = cocotb.fork(self._rxcmd_update())

    def otg_ctrl_read(self):

       return   (self.id_pullup                     << 0) | \
                (self.dp_pulldown                   << 1) | \
                (self.dm_pulldown                   << 2) | \
                (self.dischrg_vbus                  << 3) | \
                (self.chrg_vbus                     << 4) | \
                (self.drv_vbus                      << 5) | \
                (self.drv_vbus_external             << 6) | \
                (self.use_external_vbus_indicator   << 7)

    def otg_ctrl_write(self, data):

        self.id_pullup                      = (data >> 0) & 1
        self.dp_pulldown                    = (data >> 1) & 1
        self.dm_pulldown                    = (data >> 2) & 1
        self.dischrg_vbus                   = (data >> 3) & 1
        self.chrg_vbus                      = (data >> 4) & 1
        self.drv_vbus                       = (data >> 5) & 1
        self.drv_vbus_external              = (data >> 6) & 1
        self.use_external_vbus_indicator    = (data >> 7) & 1

        if self.dp_pulldown or self.dm_pulldown:
            fs_idle_event = usb.SetWire("_")
        else:
            fs_idle_event = usb.SetWire("j")

        self.rx_wire_event_queue.put_nowait(fs_idle_event)

    def register_write(self, address, data):

        if address == RegisterAddr.OTG_CTRL:
            self.otg_ctrl_write(data)
            return

    def register_read(self, address):

        if address == RegisterAddr.VENDOR_ID_LO:
            return self.vendor_id & 0xff

        if address == RegisterAddr.VENDOR_ID_HI:
            return (self.vendor_id >> 8) & 0xff

        if address == RegisterAddr.PRODUCT_ID_LO:
            return self.product_id & 0xff

        if address == RegisterAddr.PRODUCT_ID_HI:
            return (self.product_id >> 8) & 0xff

        if     address == RegisterAddr.OTG_CTRL      \
            or address == RegisterAddr.OTG_CTRL_CLR  \
            or address == RegisterAddr.OTG_CTRL_SET:

            return self.otg_ctrl_read()

        return 0x00

    async def _hw_register_write(self):
        address = self.signals.data2phy.value & ((1<<6)-1)

        self.signals.nxt    <= 1
        await RisingEdge(self.clock)
        
        if address == 0x2f:
            # Extended register write
            await RisingEdge(self.clock)
            address = self.signals.data2phy.value

        await RisingEdge(self.clock)

        data = self.signals.data2phy.value
        self.signals.nxt    <= 0

        while True:
            await RisingEdge(self.clock)
            if self.signals.stp.value == 1:
                break

        return (address, data)

    async def _hw_register_read(self):

        address = self.signals.data2phy.value & ((1<<6)-1)

        self.signals.nxt    <= 1
        await RisingEdge(self.clock)
        
        if address == 0x2f:
            # Extended register write
            await RisingEdge(self.clock)
            address = self.signals.data2phy.value

        self.signals.nxt            <= 0
        self.signals.direction      <= 1
        self.signals.data2link      <= 0
        await RisingEdge(self.clock)
        # Turn around

        self.signals.direction      <= 1
        data = self.register_read(address)
        self.signals.data2link      <= data

        await RisingEdge(self.clock)
        # Drive read data

        self.signals.direction      <= 0
        self.signals.data2link      <= 0
        await RisingEdge(self.clock)

        #Turn around

        return (address, data)

    async def _hw_transmit(self):

        transmit_cmd = self.signals.data2phy.value & ((1<<6)-1)

        if transmit_cmd == 0:
            return self._hw_transmit_no_pid(self)
        elif (transmit_cmd & 0x30):
            return self._hw_transmit_pid(self)

    async def _hw_transmit_no_pid(self):

        data = []

        self.signals.nxt        <= 1
        await RisingEdge(self.clock)
        await RisingEdge(self.clock)

        while True:
            #FIXME: Add transmission speed pacing
            #FIXME: deal with different kind of transfer types

            if self.signals.stp.value == 0:
                data.append(self.signals.data2phy.value)
            elif self.signals.data2phy.value == 0xff:
                # USB transmit error
                return []
            else:
                # USB transmit successful
                return data

    async def _hw_transmit_pid(self):

        pid = self.signals.data2phy.value & 0x0f

        self.signals.nxt        <= 1
        await RisingEdge(self.clock)
        await RisingEdge(self.clock)

        while True:
            #FIXME: Add transmission speed pacing
            #FIXME: deal with different kind of transfer types

            if self.signals.stp.value == 0:
                data.append(self.signals.data2phy.value)
            elif self.signals.data2phy.value == 0xff:
                # USB transmit error
                return []
            else:
                # USB transmit successful
                return data

    def calc_rxcmd(self, ls, sess_end, sess_valid, vbus_valid, rx_active, rx_valid, rx_error, host_disconnect):

        # ULPI 1.1 Table 7
        data = 0

        if ls == '_':
            data = data | LineState.SE0
        elif ls == 'j':
            data = data | LineState.J
        elif ls == 'k':
            data = data | LineState.K
        elif ls == '+':
            data = data | LineState.SE1

        if vbus_valid:
            data = data | (0b11 << 2)
        elif sess_valid: 
            data = data | (0b10 << 2)
        elif sess_end: 
            data = data | (0b00 << 2)
        else:
            data = data | (0b01 << 2)


        if host_disconnect:
            data = data | (0b11 << 4)
        elif rx_active and rx_error:
            data = data | (0b10 << 4)
        elif rx_active:
            data = data | (0b01 << 4)
        else:
            data = data | (0b00 << 4)

        return data

    def recalc_rxcmd(self):
        self.cur_rxcmd = self.calc_rxcmd(
                    ls              = self.cur_ls,
                    sess_end        = 0, 
                    sess_valid      = 1, 
                    vbus_valid      = 1, 
                    rx_active       = self.cur_rx_active, 
                    rx_valid        = self.cur_rx_valid, 
                    rx_error        = self.cur_rx_error, 
                    host_disconnect = 0)

        if self.cur_rxcmd != self.prev_rxcmd:
            self.rxcmd_stale    = True

        self.prev_rx_active     = self.cur_rx_active
        self.prev_rx_valid      = self.cur_rx_valid
        self.prev_rx_error      = self.cur_rx_error
        self.prev_ls            = self.cur_ls

        self.prev_rxcmd         = self.cur_rxcmd

    async def _hw_rxcmd(self):

        # Just a turn-around cycle to signal changed line state
        self.signals.direction          <= 1
        self.signals.nxt                <= 0
        self.signals.data2link          <= 0

        await RisingEdge(self.clock)

        self.signals.data2link          <= self.cur_rxcmd
        self.rxcmd_stale = False

        await RisingEdge(self.clock)

        self.signals.direction          <= 0
        self.signals.nxt                <= 0
        self.signals.data2link          <= 0

        await RisingEdge(self.clock)

    async def _hw_receive(self):
        we = await self.rx_wire_event_queue.get()

        # FIXME: hardcoded to FS right now...
        clocks_per_symbol = 5
        (rx, ls) = we.encode_as_rx_line_state(UsbSpeed.FS)

        await RisingEdge(self.clock)

        # FIXME: the code below assumes that the link won't start
        # transmitting when the phy is toggling line states but hasn't
        # asserted RxActive yet!!!
        # That's fine for well behaving host and device only!

        for i in range(len(rx)):
            clocks_remaining = clocks_per_symbol

            cur_ls  = ls[i]
            cur_rx  = rx[i]

            self.cur_ls         = cur_ls
            self.cur_rx_active  = cur_rx.rx_active
            self.cur_rx_valid   = cur_rx.rx_valid
            self.cur_rx_error   = cur_rx.rx_error
            self.cur_rx_data    = cur_rx.rx_data

            self.dut._log.info("RX: %s, LS: %s" % (cur_rx, cur_ls))

            rx_active_unchanged = self.prev_rx_active == self.cur_rx_active
            rx_active_rising    = not(self.prev_rx_active) and     self.cur_rx_active
            rx_active_falling   =     self.prev_rx_active  and not(self.cur_rx_active)

            self.recalc_rxcmd()

            if not(self.cur_rx_active) and rx_active_unchanged:

                if self.rxcmd_stale:
                    await self._hw_rxcmd()
                    clocks_remaining = clocks_remaining - 1

                for _ in range(clocks_remaining):
                    await RisingEdge(self.clock)

            else:

                if rx_active_rising:

                    # Turn-around cycle to start RX
                    self.signals.direction          <= 1
                    self.signals.nxt                <= 1
                    self.signals.data2link          <= 0
    
                    await RisingEdge(self.clock)
                    clocks_remaining = clocks_remaining - 1

                elif rx_active_falling:

                    # Turn-around cycle to finish RX
                    self.signals.direction          <= 0
                    self.signals.nxt                <= 0
                    self.signals.data2link          <= 0
    
                    await RisingEdge(self.clock)
                    clocks_remaining = clocks_remaining - 1

                if self.cur_rx_valid:
                    self.signals.nxt                <= 1
                    self.signals.data2link          <= self.cur_rx_data

                    await RisingEdge(self.clock)
                    clocks_remaining = clocks_remaining - 1

                    self.cur_rx_valid = 0
                    self.recalc_rxcmd()

                for _ in range(clocks_remaining):

                    self.signals.nxt                <= 0

                    if self.signals.direction.value == 1:
                        self.signals.data2link          <= self.cur_rxcmd
                        self.rxcmd_stale = False
                    else:
                        if self.rxcmd_stale:
                            await self._hw_rxcmd()

                    await RisingEdge(self.clock)

        pass


    async def _run(self):

        await RisingEdge(self.clock)

        # After reset, link assets stp until it's ready to operate...
        while True:
            await RisingEdge(self.clock)

            if self.signals.stp.value == 0:
                break

        while True:

            self.signals.direction      <= 0
            self.signals.nxt            <= 0

            await RisingEdge(self.clock)

            #============================================================
            # RECEIVE
            #============================================================
            if not(self.rx_wire_event_queue.empty()):
                self.dut._log.info("Packet received...")
                await self._hw_receive()
                continue

            #============================================================
            # TXCMD
            #============================================================
            if self.signals.data2phy.value != 0:

                action = self.signals.data2phy.value >> 6
    
                if action == 0x0:
                    continue
                elif action == 0x01:
                    # Transmit: link to phy
                    await self._hw_transmit()
    
                elif action == 0x2:
                    # Register write
                    (address, data) = await self._hw_register_write()
                    self.register_write(address, data)

                    self.dut._log.info("Register write: 0x%02x = 0x%02x" % (address, data))
                elif action == 0x3:
                    # Register read
                    (address, data) = await self._hw_register_read()
                    self.dut._log.info("Register read:  0x%02x = 0x%02x" % (address, data))

                continue

            #============================================================
            # RXCMD
            #============================================================
            if self.rxcmd_stale: 
                await self._hw_rxcmd()

        pass

#    async def _rxcmd_update(self):
#
#        while True:
#            await RisingEdge(self.clock)
#
#            rxcmd = self.calc_rxcmd(
#                        ls              = self.cur_ls
#                        sess_end        = 0, 
#                        sess_valid      = 1, 
#                        vbus_valid      = 1, 
#                        rx_active       = self.cur_rx_active, 
#                        rx_valid        = self.cur_valid, 
#                        rx_error        = self.cur_rx_error, 
#                        host_disconnect = 0)
#
#            if rxcmd != self.cur_rxcmd:
#                self.cur_rxcmd          = rxcmd
#                self.rxcmd_stale        = True
#
#        pass

    def receive(self, wire_event):
        # Schedule bytes to be sent to the link
        self.rx_wire_event_queue.put_nowait(wire_event)
        pass





from collections import namedtuple
from enum import IntEnum, Flag, auto

import logging

import cocotb
from cocotb.triggers import RisingEdge, Timer, First, Event

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

        if 'vendor_id' in kwargs.keys():
            self.vendor_id   = kwargs['vendor_id']
        else:
            self.vendor_id   = 0xbeef;

        if 'product_id' in kwargs.keys():
            self.product_id   = kwargs['product_id']
        else:
            self.product_id   = 0xcafe;

        super().__init__()

        if self.signals.direction is not None:
            self.signals.direction.setimmediatevalue(0)

        if self.signals.nxt is not None:
            self.signals.nxt.setimmediatevalue(0)

        if self.signals.data2link is not None:
            self.signals.data2link.setimmediatevalue(0)

        self._run_cr = None
        self._restart()

    def _restart(self):

        if  self._run_cr is not None:
            self._run_cr.kill()
        self._run_cr = cocotb.fork(self._run())

    async def _hw_register_write(self):
        #self.dut._log.info("Register write start...")
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

    def register_read(self, address):

        if address == RegisterAddr.VENDOR_ID_LO:
            return self.vendor_id & 0xff
        elif address == RegisterAddr.VENDOR_ID_HI:
            return (self.vendor_id >> 8) & 0xff
        if address == RegisterAddr.PRODUCT_ID_LO:
            return self.product_id & 0xff
        elif address == RegisterAddr.PRODUCT_ID_HI:
            return (self.product_id >> 8) & 0xff

        else:
            return 0x00

        return 

    async def _hw_register_read(self):
        #self.dut._log.info("Register read start...")
        address = self.signals.data2phy.value & ((1<<6)-1)

        self.signals.nxt    <= 1
        await RisingEdge(self.clock)
        
        if address == 0x2f:
            # Extended register write
            await RisingEdge(self.clock)
            address = self.signals.data2phy.value

        self.signals.nxt            <= 0
        self.signals.direction      <= 1
        await RisingEdge(self.clock)

        # Turn around

        await RisingEdge(self.clock)

        self.signals.data2link  <= 0x5a

        await RisingEdge(self.clock)

        self.signals.direction      <= 1

        data = self.register_read(address)
        self.signals.data2link      <= data

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

            if self.signals.data2phy.value == 0:
                continue

            action = self.signals.data2phy.value >> 6

            if action == 0x0:
                continue
            elif action == 0x01:
                # Transmit: link to phy
                await self._hw_transmit()

            elif action == 0x2:
                # Register write
                (address, data) = await self._hw_register_write()
                self.dut._log.info("Register write: 0x%02x = 0x%02x" % (address, data))
            elif action == 0x3:
                # Register read
                (address, data) = await self._hw_register_read()
                self.dut._log.info("Register read: 0x%02x = 0x%02x" % (address, data))

        pass


    def receive(self, data):
        # Schedule bytes to be sent to the link
        pass


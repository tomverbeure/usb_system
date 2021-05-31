
from collections import namedtuple
import logging

import cocotb
from cocotb.triggers import RisingEdge, Timer, First, Event

UlpiSignals = namedtuple("UlpiSignals", ['data2phy', 'data2link', 'stp', 'direction', 'nxt'])

class UlpiPhy:

    def __init__(self, dut, clock, signals, **kwargs):

        self.dut        = dut
        self.clock      = clock
        self.signals    = signals

        super().__init__(**kwargs)

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

    async def _register_write(self):
        self.dut._log.info("Register write start...")
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

    async def _register_read(self):
        self.dut._log.info("Register read start...")
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
        self.signals.data2link  <= 0x00

        return address

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

            if action == 0x2:
                # Register write
                (address, data) = await self._register_write()
                self.dut._log.info("Register write: %02x = %02x" % (address, data))
            elif action == 0x3:
                # Register read
                address = await self._register_read()
                self.dut._log.info("Register read: %02x" % (address))


        pass

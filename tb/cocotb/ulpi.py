
from collections import namedtuple
import logging

import cocotb
from cocotb.triggers import FallingEdge, Timer, First, Event

UlpiSignals = namedtuple("UlpiSignals", ['data2phy', 'data2link', 'stp', 'direction', 'nxt'])

class UlpiPhy:

    def __init__(self, signals, *args, **kwargs):
        self.log = logging.getLogger("cocotbext.ulpi.{}".format(self.__class__.__name__))
        self.log.info("ULPI PHY")

        self.signals    = signals
#        self.ulpi_data2phy      = ulpi_data2phy
#        self.ulpi_data2link     = ulpi_data2link
#        self.ulpi_stp           = ulpi_stp
#        self.ulpi_dir           = ulpi_dir
#        self.ulpi_nxt           = ulpi_nxt

        super().__init__(*args, **kwargs)

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

    async def _run(self):

        pass


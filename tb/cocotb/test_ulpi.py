# test_ulpi.py

import random
import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge

import ulpi

@cocotb.test()
async def test_ulpi_simple(dut):
    """ Test that d propagates to q """

    dut._log.info("Running test... blah.")

    # 50MHz main clock
    osc_clk = Clock(dut.osc_clk_in, 20, units="ns")  
    cocotb.fork(osc_clk.start())  # Start the clock

    # 60MHz ULPI clock
    cocotb.fork(Clock(dut.ulpi_clk, 16.6, units="ns").start())

    ulpi_signals = ulpi.UlpiSignals(
            data2phy    = dut.ulpi_data2phy,
            data2link   = dut.ulpi_data2link,
            stp         = dut.ulpi_stp,
            direction   = dut.ulpi_direction,
            nxt         = dut.ulpi_nxt
            )

    ulpi_phy = ulpi.UlpiPhy(dut, clock = dut.ulpi_clk, signals = ulpi_signals)

    for i in range(1000):
#        val = random.randint(0, 1)
#        dut.d <= val  # Assign the random value val to the input port d
        await FallingEdge(dut.osc_clk_in)
#        assert dut.q.value == val, "output q was incorrect on the {}th cycle".format(i)

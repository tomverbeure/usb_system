# test_ulpi.py

import random
import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge

import ulpi
import usb

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

    ulpi_phy = ulpi.UlpiPhy(dut, clock = dut.ulpi_clk, signals = ulpi_signals, 
            vendor_id  = 0x0451,
            product_id = 0x1507,
            )

    for i in range(3000):
        await FallingEdge(dut.osc_clk_in)


    sof = usb.SofPacket(102)
    ulpi_phy.receive(sof)
    
    for i in range(500):
        await FallingEdge(dut.osc_clk_in)


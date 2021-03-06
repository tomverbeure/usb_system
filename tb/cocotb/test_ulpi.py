# test_ulpi.py

import random
import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge

import ulpi
import usb
import pid

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


    sof = usb.SofPacket(0x219)
    ulpi_phy.receive(sof)
    
    for i in range(500):
        await FallingEdge(dut.osc_clk_in)

    setup_packet = usb.TokenPacket(pid.PID.SETUP, 0, 0)
    ulpi_phy.receive(setup_packet)

    for i in range(100):
        await FallingEdge(dut.osc_clk_in)

    data = bytes([0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x08])
    data_packet = usb.DataPacket(pid.PID.DATA0, data)
    ulpi_phy.receive(data_packet)

    for i in range(2000):
        await FallingEdge(dut.osc_clk_in)


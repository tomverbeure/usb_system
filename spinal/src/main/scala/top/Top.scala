
package top

import scala.collection.mutable.ArrayBuffer

import spinal.core._
import spinal.lib._
import spinal.lib.io._
import spinal.lib.bus.misc._
import spinal.lib.bus.amba3.apb._
import spinal.lib.com.uart._
import spinal.lib.com.jtag.Jtag

import max10._
import usb._

class Top(isSim: Boolean) extends Component 
{
    val io = new Bundle {

        val osc_clk_in      = in(Bool)

        val ulpi            = slave(Ulpi())
        val ulpi_reset_     = out(Bool)
        val ulpi_cs         = out(Bool)
        val ulpi_fault_     = in(Bool)


        val led0            = out(Bool)
        val led1            = out(Bool)
        val led2            = out(Bool)
        val led3            = out(Bool)

        val jtag            = slave(Jtag())

    }

    noIoPrefix()

    io.ulpi_reset_    := True       // FIXME: control by CPU
    io.ulpi_cs        := True
    // FIXME: wire ulpi_fault_ to CPU

    val clk_cpu       = Bool
    val clk_ulpi      = Bool
    val clk_tap       = Bool

    //============================================================
    // PLL
    //============================================================

    val u_pll = new pll()
    u_pll.inclk0      <> io.osc_clk_in
    u_pll.c0          <> clk_cpu
    u_pll.c1          <> clk_tap

    //============================================================
    // ULPI PLL
    //============================================================

    val ulpi_pll_locked   = Bool

    val u_ulpi_pll = new ulpi_pll()
    u_ulpi_pll.inclk0      <> io.ulpi.clk
    u_ulpi_pll.c0          <> clk_ulpi
    u_ulpi_pll.locked      <> ulpi_pll_locked

    //============================================================
    // Create clk cpu 
    //============================================================
    
    val clkCpuRawDomain = ClockDomain(
        clock       = clk_cpu,
        frequency   = FixedFrequency(50 MHz),
        config      = ClockDomainConfig(
            resetKind = BOOT
        )
    )

    val clk_cpu_reset_  = Bool

    val clk_cpu_reset_gen = new ClockingArea(clkCpuRawDomain) {
        val reset_unbuffered_ = True

        val reset_cntr = Reg(UInt(5 bits)) init(0)
        when(reset_cntr =/= U(reset_cntr.range -> true)){
            reset_cntr := reset_cntr + 1
            reset_unbuffered_ := False
        }

        clk_cpu_reset_ := RegNext(reset_unbuffered_)
    }


    val clkCpuDomain = ClockDomain(
        clock       = clk_cpu,
        reset       = clk_cpu_reset_,
        frequency   = FixedFrequency(50 MHz),
        config      = ClockDomainConfig(
            resetKind         = ASYNC,
            resetActiveLevel  = LOW
        )
    )

    //============================================================
    // Create ulpi clk 
    //============================================================
    
    val clkUlpiRawDomain = ClockDomain(
        clock       = clk_ulpi,
        frequency   = FixedFrequency(60 MHz),
        config      = ClockDomainConfig(
            resetKind = BOOT
        )
    )

    val clk_ulpi_reset_  = Bool

    val clk_ulpi_reset_gen = new ClockingArea(clkUlpiRawDomain) {
        val reset_unbuffered_ = True

        val reset_cntr = Reg(UInt(5 bits)) init(0)
        when(reset_cntr =/= U(reset_cntr.range -> true)){
            reset_cntr := reset_cntr + 1
            reset_unbuffered_ := False
        }

        clk_ulpi_reset_ := RegNext(reset_unbuffered_)
    }


    val clkUlpiDomain = ClockDomain(
        clock       = clk_ulpi,
        reset       = clk_ulpi_reset_,
        frequency   = FixedFrequency(60 MHz),
        config      = ClockDomainConfig(
            resetKind         = ASYNC,
            resetActiveLevel  = LOW
        )
    )

    //============================================================
    // Fast clock for signal tap
    //============================================================
    val clkTapRawDomain = ClockDomain(
        clock       = clk_tap, 
        frequency   = FixedFrequency(200 MHz),
        config      = ClockDomainConfig(
            resetKind = BOOT
        )
    )

    val led3 = new ClockingArea(clkTapRawDomain) {
        val led3 = Reg(Bool) init(False)
        led3  := ~led3;

        io.led3   := led3
    }



    //============================================================
    // CPU
    //============================================================

    val cpu = new ClockingArea(clkCpuDomain) {
        val u_cpu = new CpuTop(hasJtagUart = !isSim, hasUart = false)
        u_cpu.io.led_red        <> io.led0
        u_cpu.io.led_green      <> io.led1
        u_cpu.io.led_blue       <> io.led2
        u_cpu.io.jtag           <> io.jtag
    }

    //============================================================
    // USB
    //============================================================

    val usb = new ClockingArea(clkUlpiDomain) {

        val ulpi_internal = UlpiInternal()
        val utmi = Utmi()

        ulpi_internal.direction     := io.ulpi.direction
        ulpi_internal.nxt           := io.ulpi.nxt
        ulpi_internal.data_in       := io.ulpi.data.read

        io.ulpi.stp                 := ulpi_internal.stp
        io.ulpi.data.write          := ulpi_internal.data_out
        io.ulpi.data.writeEnable    := B(8 bits, default -> ulpi_internal.data_ena)
        
        val u_utmi2ulpi = new Utmi2UlpiWithApb(clkCpuDomain)
        u_utmi2ulpi.io.apb            <> cpu.u_cpu.io.utmi2ulpi_apb
        u_utmi2ulpi.io.ulpi           <> ulpi_internal
        u_utmi2ulpi.io.utmi           <> utmi
        u_utmi2ulpi.io.pll_locked     <> ulpi_pll_locked

        val u_usb_device = new UsbDevice()
        u_usb_device.io.utmi          <> utmi

        val usb_dev_apb = new ClockingArea(clkCpuDomain){
            val apb_regs = u_usb_device.driveFrom(Apb3SlaveFactory(cpu.u_cpu.io.usb_dev_apb), 0x0)
        }

    }

}


object TopVerilogSim {
    def main(args: Array[String]) {

        val config = SpinalConfig(anonymSignalUniqueness = true)

        config.generateVerilog({
            val toplevel = new Top(isSim = true)
            InOutWrapper(toplevel)
        })

    }
}

object TopVerilogSyn {
    def main(args: Array[String]) {

        val config = SpinalConfig(anonymSignalUniqueness = true)
        config.generateVerilog({
            val toplevel = new Top(isSim = false)
            InOutWrapper(toplevel)
            toplevel
        })
    }
}


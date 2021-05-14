
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

class Top(isSim: Boolean) extends Component 
{
    val io = new Bundle {

        val osc_clk_in      = in(Bool)

        val led0            = out(Bool)
        val led1            = out(Bool)
        val led2            = out(Bool)

        val jtag            = slave(Jtag())
    }

    noIoPrefix()

    val clk_cpu       = Bool

    clk_cpu    := io.osc_clk_in

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
            resetKind = SYNC,
            resetActiveLevel = LOW
        )
    )

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



package top

import spinal.core._
import spinal.lib._
import spinal.lib.io._
import spinal.lib.bus.misc._
import spinal.lib.bus.amba3.apb._

import max10._

object JtagUart {
    def getApb3Config() = Apb3Config(addressWidth = 12, dataWidth = 32)
}


class JtagUart extends Component {

    val io = new Bundle {
        val apb                 = slave(Apb3(JtagUart.getApb3Config()))
    }

    val av_chipselect    = Bool
    val av_waitrequest   = Bool
    val av_address       = Bool
    val av_write_n       = Bool
    val av_writedata     = Bits(32 bits)
    val av_read_n        = Bool
    val av_readdata      = Bits(32 bits)

    val u_jtag_uart = new jtag_uart()
    u_jtag_uart.clk_clk               <> ClockDomain.current.readClockWire
    u_jtag_uart.reset_reset_n         <> ClockDomain.current.readResetWire
    u_jtag_uart.av_chipselect         <> av_chipselect
    u_jtag_uart.av_address            <> av_address
    u_jtag_uart.av_waitrequest        <> av_waitrequest
    u_jtag_uart.av_write_n            <> av_write_n
    u_jtag_uart.av_writedata          <> av_writedata
    u_jtag_uart.av_read_n             <> av_read_n
    u_jtag_uart.av_readdata           <> av_readdata

    av_chipselect       := io.apb.PENABLE && io.apb.PSEL.orR
    av_address          := io.apb.PADDR(2)
    av_write_n          := !io.apb.PWRITE
    av_read_n           := io.apb.PWRITE
    av_writedata        := io.apb.PWDATA

    io.apb.PRDATA       := av_readdata
    io.apb.PREADY       := !av_waitrequest
}



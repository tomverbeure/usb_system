

package max10

import spinal.core._

class jtag_uart() extends BlackBox{

    val clk_clk             = in(Bool)
    val reset_reset_n       = in(Bool)
    val av_chipselect       = in(Bool)
    val av_address          = in(Bool)
    val av_read_n           = in(Bool)
    val av_readdata         = out(Bits(32 bits))
    val av_write_n          = in(Bool)
    val av_writedata        = in(Bits(32 bits))
    val av_waitrequest      = out(Bool)

    setDefinitionName("jtag_uart")
}




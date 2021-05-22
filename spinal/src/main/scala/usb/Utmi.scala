
package usb

import spinal.core._
import spinal.lib._
import spinal.lib.io._

object Utmi {

    object OpMode extends SpinalEnum {
        val Normal                  = 0
        val NonDriving              = 1
        val DisableBitStuffNRZI     = 2
    }

    object LineState extends SpinalEnum {
        val SE0                     = 0
        val J                       = 1
        val K                       = 2
        val SE1                     = 3
    }

    object XcvrSelect extends SpinalEnum {
        val HS                      = 0
        val FS                      = 1
        val LS                      = 2
        val LSSpecial               = 3
    }
}

case class Utmi() extends Bundle with IMasterSlave
{
    //============================================================
    // UTMI+ Level 0
    //============================================================
    val clk             = Bool

    // Only support 8-bit data
    val tx_valid        = Bool
    val tx_ready        = Bool
    val data_in         = Bits(8 bits)

    val rx_valid        = Bool
    val data_out        = Bits(8 bits)

    // Misc input
    val reset           = Bool
    val suspend_m       = Bool
    val xcvr_select     = Bits(2 bits)
    val term_select     = Bool
    val op_mode         = Bits(2 bits)

    // Misc output
    val rx_active       = Bool
    val rx_error        = Bool
    val line_state      = Bits(2 bits)

/*
    //============================================================
    // UTMI+ Level 1
    //============================================================
    
    // Output
    val host_disconnect = Bool
    val id_dig          = Bool

    val a_valid         = Bool
    val b_valid         = Bool
    val vbus_valid      = Bool
    val sess_end        = Bool
*/

    // Master = Phy, since that's how it's defined in the spec.
    override def asMaster: Unit = {

        out(clk)

        in(tx_valid)
        out(tx_ready)
        in(data_in)

        out(rx_valid)
        out(data_out)

        in(reset)
        in(suspend_m)
        in(xcvr_select)
        in(term_select)
        in(op_mode)

        out(rx_active)
        out(rx_error)
        out(line_state)
    }
}


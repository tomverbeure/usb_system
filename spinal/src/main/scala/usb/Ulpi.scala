
package usb

import spinal.core._
import spinal.lib._
import spinal.lib.io._

object Ulpi {

    // Ulpi Registers
    object UlpiAddr extends SpinalEnum {
        val VENDOR_ID_LOW                     = 0x00
        val VENDOR_ID_HIGH                    = 0x01
        val PRODUCT_ID_LOW                    = 0x02
        val PRODUCT_ID_HIGH                   = 0x03
        val FUNCTION_CONTROL                  = 0x04
        val INTERFACE_CONTROL                 = 0x07
        val OTG_CONTROL                       = 0x0a
        val USB_INTERRUPT_ENABLE_RISING       = 0x0d
        val USB_INTERRUPT_ENABLE_FALLING      = 0x10
        val USB_INTERRUPT_STATUS              = 0x13
        val USB_INTERRUPT_LATCH               = 0x14
        val DEBUG                             = 0x15
        val SCRATCH_REGISTER                  = 0x16
    }

    object UlpiCmdCode extends SpinalEnum {
        val Special         = 0
        val Transmit        = 1
        val RegWrite        = 2
        val RegRead         = 3
    }

}

case class UlpiInternal() extends Bundle with IMasterSlave
{
    // direction  = external signal that indicates to the USB device/controller how the data is going.
    // data_ena   = internal signal that controls the IO pads of the ULPI data pins
    val clk         = Bool
    val data_ena    = Bool
    val data_out    = Bits(8 bits)
    val data_in     = Bits(8 bits)
    val direction   = Bool            // 'dir' is a SpinalHDL reserved name...
    val stp         = Bool
    val nxt         = Bool
    val reset       = Bool

    // Master = Phy, since that's how it's defined in the spec.
    override def asMaster: Unit = {
        out(clk)

        // Yes, I hate that this is the opposite direction...
        in(data_ena)
        in(data_out)
        out(data_in)

        out(direction)
        in(stp)
        out(nxt)
        in(reset)
    }
}


package usb

import spinal.core._
import spinal.lib._
import spinal.lib.Reverse
import spinal.lib.io._
import spinal.lib.bus.simple._
import spinal.lib.bus.misc._
import spinal.lib.bus.amba3.apb._
import spinal.lib.com.eth.{Crc, CrcKind}

object UsbEpMem {

    def getApb3Config() = Apb3Config(addressWidth = 17, dataWidth=32)

    // Registers
    val ADDR_ENDP                 = 0x00
    val ADDR_ENDP1                = 0x04
    val ADDR_ENDP2                = 0x08
    val ADDR_ENDP3                = 0x0c
    val ADDR_ENDP4                = 0x10
    val ADDR_ENDP5                = 0x14
    val ADDR_ENDP6                = 0x18
    val ADDR_ENDP7                = 0x1c
    val ADDR_ENDP8                = 0x20
    val ADDR_ENDP9                = 0x24
    val ADDR_ENDP10               = 0x28
    val ADDR_ENDP11               = 0x2c
    val ADDR_ENDP12               = 0x30
    val ADDR_ENDP13               = 0x34
    val ADDR_ENDP14               = 0x38
    val ADDR_ENDP15               = 0x3c
    val MAIN_CTRL                 = 0x40
    val SOF_WR                    = 0x44
    val SOF_RD                    = 0x48
    val SIE_CTRL                  = 0x4c
    val SIE_STATUS                = 0x50
    val INT_EP_CTRL               = 0x54
    val BUFF_STATUS               = 0x58
    val BUFF_CPU_SHOULD_HANDLE    = 0x5c
    val EP_ABORT                  = 0x60
    val EP_ABORT_DONE             = 0x64
    val EP_STALL_ARM              = 0x68
    val NAK_POLL                  = 0x6c
    val EP_STATUS_STALL_NAK       = 0x70
    val USB_MUXING                = 0x74
    val USB_PWR                   = 0x78
    val USBPHY_DIRECT             = 0x7c
    val USBPHY_DIRECT_OVERRIDE    = 0x80
    val USBPHY_TRIM               = 0x84
    val INTR                      = 0x8c
    val INTE                      = 0x90
    val INTF                      = 0x94
    val INTS                      = 0x98
}

case class UsbEpMem(nrEndpoints : Int = 16, isSim : Boolean = false) extends Component {

    import UsbDevice._

    // Everything in this block runs at ULPI 60MHz clock speed.
    // If the APB is running at a different clock speed, use Apb3CC which is a clock crossing
    // APB bridge.

    val io = new Bundle {
        val ep_stream                     = slave(EpStream())
    }

    val ep_stream_resp = Reg(Bits(2 bits)) init(EpStream.Response.Wait.asBits)
    io.ep_stream.resp   := ep_stream_resp

    io.ep_stream.rd_data_valid  := True
    io.ep_stream.rd_data        := 0x5a
    io.ep_stream.rd_data_eop    := False

    switch(io.ep_stream.req){
        is(EpStream.Request.Setup.asBits){
            ep_stream_resp      := EpStream.Response.Ack.asBits
        }
        is(EpStream.Request.DataIn.asBits){
            ep_stream_resp      := EpStream.Response.Ack.asBits
        }
    }

    //============================================================
    // Registers
    //============================================================

    // Address map:
    // 0x0_0000 : USB DPRAM base address
    // 0x1_0000 : Register base address
    // 0x1_0094 : Last register
    // -> 17 bits address map

    def driveRegsFrom(busCtrl: BusSlaveFactory, baseAddress: BigInt) = new Area {
        
        //============================================================
        // ADDR_ENDP 
        //============================================================
        val addr_endp = new Area {
            val address             = busCtrl.createReadAndWrite(UInt(7 bits),          UsbEpMem.ADDR_ENDP, 0) init(0)
        }

        //============================================================
        // MAIN_CTRL 
        //============================================================
        val main_ctrl = new Area {
            val controller_en       = busCtrl.createReadAndWrite(Bool,                  UsbEpMem.MAIN_CTRL, 0) init(False)
            val host_ndevice        = busCtrl.createReadAndWrite(Bool,                  UsbEpMem.MAIN_CTRL, 1) init(False)
            val sim_timing          = busCtrl.createReadAndWrite(Bool,                  UsbEpMem.MAIN_CTRL, 31) init(False)
        }

        //============================================================
        // SOF_RD 
        //============================================================
        val sof_rd = new Area {
            val count               = busCtrl.createReadOnly(UInt(11 bits),             UsbEpMem.SOF_RD, 0)
        }

        //============================================================
        // SIE_CTRL 
        //============================================================
        val sie_ctrl = new Area {
            val resume              = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_CTRL, 12)  // FIXME: must be self-clearing bit (SC)
            val pullup_en           = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_CTRL, 16)
            val transceiver_pd      = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_CTRL, 18)
            val ep0_int_nak         = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_CTRL, 27)
            val ep0_int_2buf        = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_CTRL, 28)
            val ep0_int_1buf        = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_CTRL, 29)
            val ep0_double_buf      = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_CTRL, 30)
            val ep0_int_Stall       = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_CTRL, 31)
        }

        //============================================================
        // SIE_STATUS 
        //============================================================
        val sie_status = new Area {
            val vbus_detected       = busCtrl.createReadOnly(Bool,                      UsbEpMem.SIE_STATUS, 0)
            val line_state          = busCtrl.createReadOnly(Bits(2 bits),              UsbEpMem.SIE_STATUS, 2)
            val suspended           = busCtrl.createReadOnly(Bool,                      UsbEpMem.SIE_STATUS, 4)
            val vbus_over_curr      = busCtrl.createReadOnly(Bool,                      UsbEpMem.SIE_STATUS, 10)
            val resume              = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 11) // FIXME: must be clear on write
            val connected           = busCtrl.createReadOnly(Bool,                      UsbEpMem.SIE_STATUS, 16)
            val setup_rec           = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 17) // FIXME: must be clear on write
            val trans_complete      = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 18) // FIXME: must be clear on write
            val bus_reset           = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 19) // FIXME: must be clear on write
            val crc_error           = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 24) // FIXME: must be clear on write
            val bit_stuff_error     = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 25) // FIXME: must be clear on write
            val rx_overflow         = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 26) // FIXME: must be clear on write
            val rx_timeout          = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 27) // FIXME: must be clear on write
            val ack_rec             = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 30) // FIXME: must be clear on write
            val data_seq_error      = busCtrl.createReadWrite(Bool,                     UsbEpMem.SIE_STATUS, 31) // FIXME: must be clear on write
        }
  
        //============================================================
        // BUFF_STATUS 
        //============================================================
        val buff_status = new Area {

            val ep_out_buff_status  = Bits(nrEndpoints bits)
            val ep_in_buff_status   = Bits(nrEndpoints bits)

            for(ep_nr <- 0 to nrEndpoints-1){
                ep_in_buff_status(ep_nr)  := busCtrl.createReadOnly(Bool,               UsbEpMem.BUFF_STATUS,  ep_nr * 2   )
                ep_out_buff_status(ep_nr) := busCtrl.createReadOnly(Bool,               UsbEpMem.BUFF_STATUS, (ep_nr * 2)+1)
            }
        }

        //============================================================
        // BUFF_CPU_SHOULD_HANDLE 
        //============================================================
        val buff_cpu_should_handle = new Area {

            val ep_out_cpu_should_handle  = Bits(nrEndpoints bits)
            val ep_in_cpu_should_handle   = Bits(nrEndpoints bits)

            for(ep_nr <- 0 to nrEndpoints-1){
                ep_in_cpu_should_handle(ep_nr)  := busCtrl.createReadOnly(Bool,         UsbEpMem.BUFF_STATUS,  ep_nr * 2   )
                ep_out_cpu_should_handle(ep_nr) := busCtrl.createReadOnly(Bool,         UsbEpMem.BUFF_STATUS, (ep_nr * 2)+1)
            }
        }

        //============================================================
        // EP_ABORT 
        //============================================================
        val ep_abort = new Area {

            val ep_out_abort  = Bits(nrEndpoints bits)
            val ep_in_abort   = Bits(nrEndpoints bits)

            for(ep_nr <- 0 to nrEndpoints-1){
                ep_in_abort(ep_nr)  := busCtrl.createReadWrite(Bool,                    UsbEpMem.EP_ABORT,  ep_nr * 2   )
                ep_out_abort(ep_nr) := busCtrl.createReadWrite(Bool,                    UsbEpMem.EP_ABORT, (ep_nr * 2)+1)
            }
        }

        //============================================================
        // EP_ABORT_DONE
        //============================================================
        val ep_abort_done = new Area {

            val ep_out_abort_done  = Bits(nrEndpoints bits)
            val ep_in_abort_done   = Bits(nrEndpoints bits)

            for(ep_nr <- 0 to nrEndpoints-1){
                // FIXME: make clear on write
                ep_in_abort_done(ep_nr)  := busCtrl.createReadWrite(Bool,               UsbEpMem.EP_ABORT_DONE,  ep_nr * 2   )
                ep_out_abort_done(ep_nr) := busCtrl.createReadWrite(Bool,               UsbEpMem.EP_ABORT_DONE, (ep_nr * 2)+1)
            }
        }

        //============================================================
        // EP_STATUS_STALL_NAK
        //============================================================
        val ep_status_stall_nak = new Area {

            val ep_out_stall_nak  = Bits(nrEndpoints bits)
            val ep_in_stall_nak   = Bits(nrEndpoints bits)

            for(ep_nr <- 0 to nrEndpoints-1){
                // FIXME: make clear on write
                ep_in_stall_nak(ep_nr)  := busCtrl.createReadWrite(Bool,                UsbEpMem.EP_STATUS_STALL_NAK,  ep_nr * 2   )
                ep_out_stall_nak(ep_nr) := busCtrl.createReadWrite(Bool,                UsbEpMem.EP_STATUS_STALL_NAK, (ep_nr * 2)+1)
            }
        }

        //============================================================
        // INTR
        //============================================================
        val intr = new Area {
            val trans_complete        = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 3)
            val buff_status           = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 4)
            val error_data_seq        = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 5)
            val error_rx_timeout      = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 6)
            val error_rx_overflow     = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 7)
            val error_bit_stuff       = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 8)
            val error_crc             = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 9)
            val stall                 = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 10)
            val vbus_detect           = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 11)
            val bus_reset             = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 12)
            val dev_conn_dis          = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 13)
            val dev_suspend           = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 14)
            val dev_resume_from_host  = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 15)
            val setup_req             = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 16)
            val dev_sof               = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 17)
            val abort_done            = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 18)
            val ep_stall_nak          = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 19)
        }
  
        //============================================================
        // INTE
        //============================================================
        val inte = new Area {
            val trans_complete        = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 3)
            val buff_status           = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 4)
            val error_data_seq        = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 5)
            val error_rx_timeout      = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 6)
            val error_rx_overflow     = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 7)
            val error_bit_stuff       = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 8)
            val error_crc             = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 9)
            val stall                 = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 10)
            val vbus_detect           = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 11)
            val bus_reset             = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 12)
            val dev_conn_dis          = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 13)
            val dev_suspend           = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 14)
            val dev_resume_from_host  = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 15)
            val setup_req             = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 16)
            val dev_sof               = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 17)
            val abort_done            = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 18)
            val ep_stall_nak          = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 19)
        }

        //============================================================
        // INTF
        //============================================================
        val intf = new Area {
            val trans_complete        = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 3)
            val buff_status           = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 4)
            val error_data_seq        = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 5)
            val error_rx_timeout      = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 6)
            val error_rx_overflow     = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 7)
            val error_bit_stuff       = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 8)
            val error_crc             = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 9)
            val stall                 = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 10)
            val vbus_detect           = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 11)
            val bus_reset             = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 12)
            val dev_conn_dis          = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 13)
            val dev_suspend           = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 14)
            val dev_resume_from_host  = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 15)
            val setup_req             = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 16)
            val dev_sof               = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 17)
            val abort_done            = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 18)
            val ep_stall_nak          = busCtrl.createReadWrite(Bool,                           UsbEpMem.INTR, 19)
        }

        //============================================================
        // INTS
        //============================================================
        val ints = new Area {
            val trans_complete        = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 3)
            val buff_status           = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 4)
            val error_data_seq        = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 5)
            val error_rx_timeout      = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 6)
            val error_rx_overflow     = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 7)
            val error_bit_stuff       = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 8)
            val error_crc             = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 9)
            val stall                 = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 10)
            val vbus_detect           = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 11)
            val bus_reset             = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 12)
            val dev_conn_dis          = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 13)
            val dev_suspend           = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 14)
            val dev_resume_from_host  = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 15)
            val setup_req             = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 16)
            val dev_sof               = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 17)
            val abort_done            = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 18)
            val ep_stall_nak          = busCtrl.createReadOnly(Bool,                            UsbEpMem.INTR, 19)
        }
    }

}

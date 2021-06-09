package usb

import spinal.core._
import spinal.lib._
import spinal.lib.Reverse
import spinal.lib.io._
import spinal.lib.bus.simple._
import spinal.lib.bus.misc._
import spinal.lib.bus.amba3.apb._
import spinal.lib.com.eth.{Crc, CrcKind}

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


}

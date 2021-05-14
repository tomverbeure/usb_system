
package cc

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.io.{TriStateArray, TriState}

object CCMiscCtrlStatus {
    def getApb3Config() = Apb3Config(addressWidth = 5,dataWidth = 32)
}

case class CCMiscCtrlStatus(nrCtrls: Int, nrStatus: Int) extends Component {

    val io = new Bundle {
        val apb     = slave(Apb3(CCMiscCtrlStatus.getApb3Config()))

        val ctrl    = if (nrCtrls > 0)  out(Bits(nrCtrls bits)) else null
        val status  = if (nrStatus > 0) in(Bits(nrStatus bits)) else null
    }

    val regs  = Apb3SlaveFactory(io.apb)

    if (nrCtrls > 0){
        io.ctrl     := regs.createReadAndWrite(Bits(nrCtrls bits), 0x00, 0) init(0)
    }

    if (nrStatus > 0){
        regs.read(io.status, 0x04)
    }
}

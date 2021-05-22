
package usb

import spinal.core._
import spinal.lib._
import spinal.lib.bus.misc._
import spinal.lib.bus.amba3.apb._

import Ulpi.{UlpiAddr,UlpiCmdCode}
import Utmi.{OpMode}

object Utmi2Ulpi {

    def getApb3Config() = Apb3Config(addressWidth = 8, dataWidth=32)

}

case class Utmi2Ulpi() extends Component {

    // Everything in this block runs at ULPI 60MHz clock speed.
    val io = new Bundle {
        val ulpi            = slave(UlpiInternal())
        val utmi            = master(Utmi())

        // The spec isn't very clear about when to use PHY reset of the FUNCTION_CONTROL register.
        // So simply defer it to higher authorities...
        val func_reset      = in(Bool)

        val reg_rd_ena      = in(Bool)
        val reg_rd_done     = out(Bool)
        val reg_rd_addr     = in(UInt(5 bits))
        val reg_rd_data     = out(Bits(8 bits))
    }

    object UlpiState extends SpinalEnum {
        val Idle                = newElement()
        val TxStart             = newElement()
        val TxData              = newElement()
        val RegWrWaitAddrAck    = newElement()
        val RegWrWaitDataAck    = newElement()
        val RegWrStp            = newElement()
        val RegRdWaitAddrAck    = newElement()
        val RegRdTurnaround     = newElement()
        val RegRdData           = newElement()
    }

    val utmi_xcvr_select  = RegNext(io.utmi.xcvr_select)
    val utmi_term_select  = RegNext(io.utmi.term_select)
    val utmi_op_mode      = RegNext(io.utmi.op_mode)
    val utmi_suspend_m    = RegNext(io.utmi.suspend_m)
    val func_reset        = RegNext(io.func_reset)

    val turn_around     = (io.ulpi.direction =/= RegNext(io.ulpi.direction))

    val func_ctrl_diff =  ( (utmi_xcvr_select =/= io.utmi.xcvr_select) 
                          | (utmi_term_select =/= io.utmi.term_select) 
                          | (utmi_op_mode     =/= io.utmi.op_mode) 
                          | (utmi_suspend_m   =/= io.utmi.suspend_m) )

    val func_ctrl_wr_pending  = Reg(Bool) setWhen(func_ctrl_diff)

    val reg_rd_pending = Reg(Bool) init(False) setWhen(io.reg_rd_ena)

//    io.ulpi.data_ena    := !turn_around && !io.ulpi.direction
    io.ulpi.data_ena    := !io.ulpi.direction

    val ulpi_data_out   = Reg(Bits(8 bits)) init(0)
    val ulpi_stp        = Reg(Bool) init(True)
    io.ulpi.data_out    := ulpi_data_out
    io.ulpi.stp         := ulpi_stp

    val utmi_rx_active        = Reg(Bool) init(False)
    val utmi_rx_error         = Reg(Bool) init(False)
    val utmi_rx_valid         = Reg(Bool) init(False)
    val utmi_rx_data          = Reg(Bits(8 bits)) init(0)
    val utmi_tx_ready         = Bool
    val utmi_line_state       = Reg(Bits(2 bits)) init(0)
    val utmi_sess_end         = Reg(Bool) init(False)
    val utmi_sess_valid       = Reg(Bool) init(False)
    val utmi_vbus_valid       = Reg(Bool) init(False)
    val utmi_host_disconnect  = Reg(Bool) init(False)
    val utmi_id_dig           = Reg(Bool) init(False)

    io.utmi.rx_active     := utmi_rx_active
    io.utmi.rx_error      := utmi_rx_error
    io.utmi.rx_valid      := utmi_rx_valid
    io.utmi.data_out      := utmi_rx_data
    io.utmi.tx_ready      := utmi_tx_ready
    io.utmi.line_state    := utmi_line_state

    val reg_wr_data     = Reg(Bits(8 bits)) init(0)

    val reg_rd_done     = Reg(Bool) init(False)
    val reg_rd_data     = Reg(Bits(8 bits)) init(0)

    io.reg_rd_done  := reg_rd_done
    io.reg_rd_data  := reg_rd_data

    val cur_state = Reg(UlpiState()) init(UlpiState.Idle)

    val reg_wr_ongoing = cur_state === UlpiState.RegWrWaitAddrAck || cur_state === UlpiState.RegWrWaitDataAck

    ulpi_stp            := False
    utmi_tx_ready       := False
    reg_rd_done         := False

    when(turn_around){

        when(io.ulpi.direction && io.ulpi.nxt){
            when(reg_wr_ongoing){
                // ULPI 3.8.3.2: RegWr abort
                cur_state     := UlpiState.Idle
            }

            utmi_rx_active    := True
        }
        .elsewhen(!io.ulpi.direction){
            // ULPI 3.8.2.4: PHY stops sending info
            utmi_rx_active    := False
        }

    }
    .elsewhen(io.ulpi.direction){
        // No FSM needed when receiving data from ULPI
        when(!io.ulpi.nxt){
            // ULPI 3.8.1.2: RX CMD
            utmi_line_state   := io.ulpi.data_in(1 downto 0)
            switch(io.ulpi.data_in(3 downto 2)){
                is(0){
                    utmi_sess_end         := True
                    utmi_sess_valid       := False
                    utmi_vbus_valid       := False
                }
                is(1){
                    utmi_sess_end         := False
                    utmi_sess_valid       := False
                    utmi_vbus_valid       := False
                }
                is(2){
                    utmi_sess_end         := False
                    utmi_sess_valid       := True
                    utmi_vbus_valid       := False
                }
                is(3){
                    utmi_sess_end         := False
                    utmi_sess_valid       := False
                    utmi_vbus_valid       := True
                }
            }
            switch(io.ulpi.data_in(5 downto 4)){
                is(0){
                    utmi_rx_active        := False
                    utmi_rx_error         := False
                    utmi_host_disconnect  := False
                }
                is(1){
                    utmi_rx_active        := True
                    utmi_rx_error         := False
                    utmi_host_disconnect  := False
                }
                is(2){
                    utmi_rx_active        := False
                    utmi_rx_error         := False
                    utmi_host_disconnect  := True
                }
                is(3){
                    utmi_rx_active        := True
                    utmi_rx_error         := True
                    utmi_host_disconnect  := False
                }
            }
            utmi_id_dig       := io.ulpi.data_in(6)
        }
        .otherwise{
            utmi_rx_valid     := True
            utmi_rx_data      := io.ulpi.data_in
        }
    }
    .otherwise{
        when(!io.ulpi.direction){
            // We are driving the bus...
            switch(cur_state){
                is(UlpiState.Idle){
                    // State changes have priority over transmitting data
                    when(func_ctrl_wr_pending){
                        ulpi_data_out       := B(UlpiCmdCode.RegWrite, 2 bits) ## B(UlpiAddr.FUNCTION_CONTROL, 6 bits)
                        reg_wr_data         := False ## utmi_suspend_m ## func_reset ## utmi_op_mode ## utmi_term_select ## utmi_xcvr_select

                        cur_state           := UlpiState.RegWrWaitAddrAck
                    }
                    .elsewhen(io.utmi.tx_valid){
                        // ULPI 3.8.1.1: TX_CMD
                        ulpi_data_out       := B(UlpiCmdCode.Transmit, 2 bits) ## B(0, 2 bits) ## io.utmi.data_in(3 downto 0)
                        cur_state           := UlpiState.TxStart
                    }
                    when(reg_rd_pending){
                        ulpi_data_out       := B(UlpiCmdCode.RegRead, 2 bits) ## B(UlpiAddr.VENDOR_ID_LOW, 6 bits)
                        reg_rd_pending      := False

                        cur_state           := UlpiState.RegRdWaitAddrAck
                    }

                    // FIXME: Add option for register reads by upper layers
                }
                is(UlpiState.TxStart){
                    // tx_valid should be high here at all times, because we didn't
                    // assert tx_ready in the previous cycle to pop the first byte

                    // Already pop tx data so that the next value is available
                    // when ulpi.nxt is asserted in the next stage.
                    utmi_tx_ready       := True
                    cur_state           := UlpiState.TxData
                }
                is(UlpiState.TxData){
                    when(io.ulpi.nxt){
                        // We have a combinatorial path from ulpi.nxt to utmi.tx_ready.
                        // We can break this with a skidding stage if really necessary...

                        when(io.utmi.tx_valid && io.utmi.op_mode === B(OpMode.DisableBitStuffNRZI, 2 bits) && io.utmi.data_in === 0xff){
                            // ULPI 3.8.2.3: USB Transmit Error
                            ulpi_data_out       := io.utmi.data_in
                            ulpi_stp            := True
                            // According to figure 13, there is no extra TxReady. There should also
                            // not be an extra TxValid. So go immediately back to Idle.
                            cur_state           := UlpiState.Idle
                        }
                        .elsewhen(io.utmi.tx_valid){
                            // 
                            ulpi_data_out       := io.utmi.data_in
                            utmi_tx_ready       := True
                            cur_state           := UlpiState.TxData
                        }
                        .otherwise{
                            ulpi_data_out       := 0
                            ulpi_stp            := True

                            // Assert tx_ready one more cycle because UTMI is stupid that way...
                            utmi_tx_ready       := True
                            cur_state           := UlpiState.Idle
                        }
                    }
                }
                is(UlpiState.RegWrWaitAddrAck){
                    // ULPI 3.8.3.1
                    when (io.ulpi.nxt){
                        ulpi_data_out       := reg_wr_data

                        cur_state           := UlpiState.RegWrWaitDataAck
                    }
                }
                is(UlpiState.RegWrWaitDataAck){
                    // ULPI 3.8.3.1
                    when (io.ulpi.nxt){
                        ulpi_stp            := True
                        ulpi_data_out       := 0

                        when(func_ctrl_wr_pending && !func_ctrl_diff){
                            func_ctrl_wr_pending  := False
                        }

                        cur_state           := UlpiState.Idle
                    }
                }

                is(UlpiState.RegRdWaitAddrAck){
                    // ULPI 3.8.3.1
                    when (io.ulpi.nxt){
                        ulpi_data_out       := 0

                        cur_state           := UlpiState.RegRdTurnaround
                    }
                }

                is(UlpiState.RegRdTurnaround){
                    cur_state           := UlpiState.RegRdData
                }
                is(UlpiState.RegRdData){
                    reg_rd_data         := io.ulpi.data_in
                    reg_rd_done         := True
                    cur_state           := UlpiState.Idle
                }

            }
        }
    }

    def driveFrom(busCtrl: BusSlaveFactory, baseAddress: BigInt) = new Area {
        
      //============================================================
      // ULPI Register Access 
      //============================================================
      val reg_req                   = busCtrl.createReadAndWrite(Bool,          0x00, 0)
      val reg_rd_ena                = busCtrl.createReadAndWrite(Bool,          0x00, 1)
      val reg_pending               = busCtrl.createReadOnly(    Bool,          0x00, 2)
      val reg_addr                  = busCtrl.createReadAndWrite(Bits(8 bits),  0x00, 8)
      val reg_wr_data               = busCtrl.createReadAndWrite(Bits(8 bits),  0x00, 16)
      val reg_rd_data               = busCtrl.createReadOnly(    Bits(8 bits),  0x00, 24)
    }
}

object Utmi2UlpiVerilog{
    def main(args: Array[String]) {

        val config = SpinalConfig(anonymSignalUniqueness = true)
        config.includeFormal.generateSystemVerilog({
            val toplevel = new Utmi2Ulpi()
            toplevel
        })
        println("DONE")
    }
}


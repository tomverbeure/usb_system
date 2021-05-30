
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

        // Overall function enable.
        // Prevents the whole thing from starting to run before the CPU gives the go ahead.
        val enable          = in(Bool)

        // The spec isn't very clear about when to use PHY reset of the FUNCTION_CONTROL register.
        // So simply defer it to higher authorities...
        val func_reset      = in(Bool)

        val reg_req         = in(Bool)
        val reg_req_done    = out(Bool)
        val reg_rd          = in(Bool)
        val reg_addr        = in(UInt(8 bits))
        val reg_wdata       = in(Bits(8 bits))
        val reg_rdata       = out(Bits(8 bits))
    }

    object UlpiState extends SpinalEnum {
        val Idle                = newElement()
        val TxStart             = newElement()
        val TxData              = newElement()
        val RegWrWaitAddrAck    = newElement()
        val RegWrWaitDataAck    = newElement()
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
                          | (utmi_suspend_m   =/= io.utmi.suspend_m) 
                          | (func_reset       =/= io.func_reset) )

    val func_ctrl_wr_pending  = Reg(Bool) init(True) setWhen(func_ctrl_diff)


    io.ulpi.data_ena    := !io.ulpi.direction

    val ulpi_data_in      = Bits(8 bits)
    val ulpi_data_out     = Reg(Bits(8 bits)) init(0)
    val ulpi_stp          = Reg(Bool) init(True)

    ulpi_data_in          := io.ulpi.data_in
    io.ulpi.data_out      := ulpi_data_out
    io.ulpi.stp           := ulpi_stp

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

    val reg_req_pending = Reg(Bool) init(False) setWhen(io.reg_req && !RegNext(io.reg_req) ) clearWhen(!io.reg_req)
    val reg_req_ongoing = Reg(Bool) init(False)
    val reg_req_done    = Reg(Bool) init(False) clearWhen(!io.reg_req)
    val reg_rdata       = Reg(Bits(8 bits)) init(0)

    io.reg_req_done     := reg_req_done
    io.reg_rdata        := reg_rdata

    val cur_state = Reg(UlpiState()) init(UlpiState.Idle)

    val reg_wr_ongoing = cur_state === UlpiState.RegWrWaitAddrAck || cur_state === UlpiState.RegWrWaitDataAck
    val reg_wr_data     = Reg(Bits(8 bits)) init(0)

    ulpi_stp            := False
    utmi_tx_ready       := False
    utmi_rx_valid       := False

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
    .elsewhen(io.ulpi.direction && cur_state =/= UlpiState.RegRdData){
        // No FSM needed when receiving data from ULPI
        when(!io.ulpi.nxt){
            // ULPI 3.8.1.2: RX CMD
            utmi_line_state   := ulpi_data_in(1 downto 0)
            switch(ulpi_data_in(3 downto 2)){
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
            switch(ulpi_data_in(5 downto 4)){
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
            utmi_id_dig       := ulpi_data_in(6)
        }
        .otherwise{
            utmi_rx_valid     := True
            utmi_rx_data      := ulpi_data_in
        }
    }
    .otherwise{
        switch(cur_state){
            is(UlpiState.Idle){
                when(!io.enable){
                    // Dummy...
                }
                // State changes have priority over transmitting data
                .elsewhen(func_ctrl_wr_pending){
                    ulpi_data_out       := B(UlpiCmdCode.RegWrite, 2 bits) ## B(UlpiAddr.FUNCTION_CONTROL, 6 bits)
                    reg_wr_data         := False ## utmi_suspend_m ## func_reset ## utmi_op_mode ## utmi_term_select ## utmi_xcvr_select

                    cur_state           := UlpiState.RegWrWaitAddrAck
                }
                .elsewhen(io.utmi.tx_valid){
                    // ULPI 3.8.1.1: TX_CMD
                    ulpi_data_out       := B(UlpiCmdCode.Transmit, 2 bits) ## B(0, 2 bits) ## io.utmi.data_in(3 downto 0)
                    cur_state           := UlpiState.TxStart
                }
                .elsewhen(reg_req_pending && io.reg_rd){
                    ulpi_data_out       := B(UlpiCmdCode.RegRead, 2 bits) ## io.reg_addr(5 downto 0)
                    reg_req_pending     := False
                    reg_req_ongoing     := True

                    cur_state           := UlpiState.RegRdWaitAddrAck
                }
                .elsewhen(reg_req_pending && !io.reg_rd){
                    ulpi_data_out       := B(UlpiCmdCode.RegWrite, 2 bits) ## io.reg_addr(5 downto 0)
                    reg_wr_data         := io.reg_wdata
                    reg_req_pending     := False
                    reg_req_ongoing     := True

                    cur_state           := UlpiState.RegWrWaitAddrAck
                }
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
                    .elsewhen(reg_req_ongoing){
                        reg_req_ongoing := False
                        reg_req_done    := True
                    }

                    cur_state           := UlpiState.Idle
                }
            }

            is(UlpiState.RegRdWaitAddrAck){
                // ULPI 3.8.3.1
                when (io.ulpi.nxt){
                    ulpi_data_out       := 0

                    cur_state           := UlpiState.RegRdData
                }
            }
            is(UlpiState.RegRdTurnaround){
                cur_state           := UlpiState.RegRdData
            }
            is(UlpiState.RegRdData){
                reg_rdata           := ulpi_data_in
                reg_req_ongoing     := False
                reg_req_done        := True
                cur_state           := UlpiState.Idle
            }

        }
    }

}

case class Utmi2UlpiWithApb(apbClkDomain : ClockDomain) extends Component {

    val io = new Bundle {
        val ulpi            = slave(UlpiInternal())
        val utmi            = master(Utmi())

        val pll_locked      = in(Bool)

        val apb             = slave(Apb3(Utmi2Ulpi.getApb3Config()))
    }

    val u_utmi2ulpi = new Utmi2Ulpi()
    u_utmi2ulpi.io.ulpi           <> io.ulpi
    u_utmi2ulpi.io.utmi           <> io.utmi

    val apb_regs = new ClockingArea(apbClkDomain) {
        val busCtrl = Apb3SlaveFactory(io.apb)

        //============================================================
        // Config
        //============================================================
        val enable                    = busCtrl.createReadAndWrite(Bool,          0x0000, 0) init(False)
        val func_reset                = busCtrl.createReadAndWrite(Bool,          0x0000, 1) init(False)

        //============================================================
        // Status
        //============================================================
        val pll_locked                = busCtrl.createReadOnly    (Bool,          0x0004, 0) init(False)

        pll_locked              := BufferCC(io.pll_locked)

        //============================================================
        // ULPI Register Access 
        //============================================================
        val reg_req                   = busCtrl.createReadAndWrite(Bool,          0x0008, 0)
        val reg_req_done              = busCtrl.createReadOnly(    Bool,          0x0008, 1)
        val reg_rd                    = busCtrl.createReadAndWrite(Bool,          0x0008, 2)
        val reg_addr                  = busCtrl.createReadAndWrite(UInt(8 bits),  0x0008, 8)
        val reg_wdata                 = busCtrl.createReadAndWrite(Bits(8 bits),  0x0008, 16)
        val reg_rdata                 = busCtrl.createReadOnly(    Bits(8 bits),  0x0008, 24)

        reg_req_done                := BufferCC(u_utmi2ulpi.io.reg_req_done)
        reg_rdata                   := u_utmi2ulpi.io.reg_rdata.addTag(crossClockDomain)
    }

    u_utmi2ulpi.io.enable     := BufferCC(apb_regs.enable)
    u_utmi2ulpi.io.func_reset := BufferCC(apb_regs.func_reset)

    u_utmi2ulpi.io.reg_req      := BufferCC(apb_regs.reg_req)
    u_utmi2ulpi.io.reg_rd       := apb_regs.reg_rd.addTag(crossClockDomain)
    u_utmi2ulpi.io.reg_addr     := apb_regs.reg_addr.addTag(crossClockDomain)
    u_utmi2ulpi.io.reg_wdata    := apb_regs.reg_wdata.addTag(crossClockDomain)

}


object Utmi2UlpiVerilog{
    def main(args: Array[String]) {

        val config = SpinalConfig(anonymSignalUniqueness = true)
        config.includeFormal.generateVerilog({
            val toplevel = new Utmi2Ulpi()
            toplevel
        })
        println("DONE")
    }
}


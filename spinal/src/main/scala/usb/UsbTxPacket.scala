package usb

import spinal.core._
import spinal.lib._
import spinal.lib.Reverse
import spinal.lib.io._
import spinal.lib.bus.simple._
import spinal.lib.bus.misc._
import spinal.lib.bus.amba3.apb._
import spinal.lib.com.eth.{Crc, CrcKind}

case class UsbTxPacket(isSim : Boolean = false) extends Component {

    import UsbDevice._

    val io = new Bundle {

        val utmi_tx_valid         = out(Bool)
        val utmi_tx_ready         = in(Bool)
        val utmi_tx_data          = out(Bits(8 bits))

        val pkt_pid               = in(Bits(4 bits))
        val pkt_data_valid        = in(Bool)
        val pkt_data_ready        = out(Bool)
        val pkt_data              = in(Bits(8 bits))

    }

    //============================================================
    // Tx Packet FSM
    //============================================================

    val tx_fsm = new Area {

        val utmi_tx_valid   = Reg(Bool) init(False)
        val utmi_tx_data    = Reg(Bits(8 bits)) init(0)

        io.utmi_tx_valid    := utmi_tx_valid
        io.utmi_tx_data     := utmi_tx_data

        object TxState extends SpinalEnum {
            val Idle            = newElement()
            val XmitPid         = newElement()    
            val XmitData        = newElement()
            val XmitCrc16Lsb    = newElement()
            val XmitCrc16Msb    = newElement()
        }

        val u_crc16 = Crc(CrcKind.usb.crc16, 8)
        u_crc16.io.input.valid   := False
        u_crc16.io.input.payload := io.pkt_data
        u_crc16.io.flush         := False

        val data_crc16_match    = u_crc16.io.result === B(0x4ffe, 16 bits)

        val tx_state = Reg(TxState()) init(TxState.Idle)

        io.pkt_data_ready       := False

        switch(tx_state){
            is(TxState.Idle){
                when(io.pkt_pid =/= PidType.NULL.asBits){
                    utmi_tx_valid       := True
                    utmi_tx_data        := ~io.pkt_pid ## io.pkt_pid

                    u_crc16.io.flush    := True

                    tx_state            := TxState.XmitPid
                }
            }
            is(TxState.XmitPid){
                when(io.utmi_tx_ready){
                    when(   io.pkt_pid === PidType.DATA0.asBits 
                         || io.pkt_pid === PidType.DATA1.asBits 
                         || io.pkt_pid === PidType.DATA2.asBits 
                         || io.pkt_pid === PidType.MDATA.asBits)
                    {
                        when(!io.pkt_data_valid){
                            utmi_tx_valid           := True
                            utmi_tx_data            := u_crc16.io.result(7 downto 0)

                            tx_state                := TxState.XmitCrc16Lsb
                        }
                        .otherwise{
                            utmi_tx_valid           := True
                            u_crc16.io.input.valid  := True
                            io.pkt_data_ready       := True

                            tx_state                := TxState.XmitData
                        }
                    }
                    .otherwise{
                        utmi_tx_valid   := False
                        utmi_tx_data    := 0

                        tx_state        := TxState.Idle
                    }
                }
            }
            is(TxState.XmitData){
                when(io.utmi_tx_ready){
                    when(!io.pkt_data_valid){
                        utmi_tx_valid           := True
                        utmi_tx_data            := u_crc16.io.result(7 downto 0)

                        tx_state                := TxState.XmitCrc16Lsb
                    }
                    .otherwise{
                        utmi_tx_valid           := True
                        u_crc16.io.input.valid  := True
                        io.pkt_data_ready       := True
                    }
                }
            }
            is(TxState.XmitCrc16Lsb){
                when(io.utmi_tx_ready){
                    utmi_tx_valid           := True
                    utmi_tx_data            := u_crc16.io.result(15 downto 8)

                    tx_state                := TxState.XmitCrc16Msb
                }
            }
            is(TxState.XmitCrc16Msb){
                when(io.utmi_tx_ready){
                    tx_state                := TxState.Idle
                }
            }

        }

    }

}



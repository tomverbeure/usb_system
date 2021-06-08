package usb

import spinal.core._
import spinal.lib._
import spinal.lib.Reverse
import spinal.lib.io._
import spinal.lib.bus.simple._
import spinal.lib.bus.misc._
import spinal.lib.bus.amba3.apb._
import spinal.lib.com.eth.{Crc, CrcKind}

case class UsbRxPacket(isSim : Boolean = false) extends Component {

    import UsbDevice._

    val io = new Bundle {

        val utmi_rx_active        = in(Bool)
        val utmi_rx_valid         = in(Bool)
        val utmi_rx_error         = in(Bool)
        val utmi_rx_data          = in(Bits(8 bits))

        val pkt_start             = out(Bool)
        val pkt_end               = out(Bool)
        val pkt_active            = out(Bool)

        // trans_err can happen when the lower PID nibble doesn't match the inverted upper PID nibble,
        // or when crc5 of a token packet is incorrect.
        // It's only valid together with pkt_end.
        val pkt_trans_err         = out(Bool)

        val pkt_pid               = out(Bits(4 bits))

        val pkt_is_token          = out(Bool)
        val pkt_addr              = out(UInt(7 bits))
        val pkt_endp              = out(UInt(4 bits))

        val pkt_is_sof            = out(Bool)
        val pkt_frame_nr          = out(UInt(11 bits))

        val pkt_is_data           = out(Bool)
        val pkt_data_valid        = out(Bool)
        val pkt_data              = out(Bits(8 bits))

        val crc5_err              = out(Bool)
        val crc16_err             = out(Bool)
    }

    //============================================================
    // Rx Packet FSM
    //============================================================

    val rx_fsm = new Area {

        val pid_value       =  io.utmi_rx_data(3 downto 0)
        val pid_value_alt   = ~io.utmi_rx_data(7 downto 4)
        val pid_valid       = pid_value === pid_value_alt

        val cur_pkt_pid         = Reg(Bits(4 bits)) init(0)
        val cur_pkt_token_data  = Reg(Bits(11 bits)) init(0)
        val cur_pkt_crc5        = Reg(Bits(5 bits)) init(0)

        io.pkt_pid          := cur_pkt_pid

        io.pkt_is_token     := False
        io.pkt_is_sof       := False
        io.pkt_is_data      := False

        io.pkt_addr         := cur_pkt_token_data(6 downto 0).asUInt
        io.pkt_endp         := cur_pkt_token_data(10 downto 7).asUInt
        io.pkt_frame_nr     := cur_pkt_token_data.asUInt
        io.pkt_data_valid   := False
        io.pkt_data         := io.utmi_rx_data

        io.crc5_err         := False
        io.crc16_err        := False

        object RxState extends SpinalEnum {
            val Idle                    = newElement()
            val WaitPid                 = newElement()
            val WaitData                = newElement()
            val WaitToken1              = newElement()
            val WaitToken2              = newElement()
            val TokenCrcCheck           = newElement()
            val WaitPacketDone          = newElement()
        }

        val u_crc5 = Crc(CrcKind.usb.crc5, 11)
        u_crc5.io.input.valid   := False
        u_crc5.io.input.payload := 0
        u_crc5.io.flush         := False

        val rx_state = Reg(RxState()) init(RxState.Idle)

        val token_crc5_calc = u_crc5.io.result
        val token_crc5_match = token_crc5_calc === cur_pkt_crc5

        val pkt_start     = Bool
        val pkt_end       = Bool
        val pkt_active    = Reg(Bool) init(False)
        val pkt_trans_err = Bool

        io.pkt_start      := pkt_start
        io.pkt_end        := pkt_end
        io.pkt_active     := pkt_active
        io.pkt_trans_err  := pkt_trans_err

        pkt_start       := False
        pkt_end         := False
        pkt_trans_err   := False

        switch(rx_state){
            is(RxState.Idle){
                when(io.utmi_rx_active){
                    pkt_start       := True
                    pkt_active      := True
                    cur_pkt_pid     := PidType.NULL.asBits

                    rx_state        := RxState.WaitPid
                }
            }

            is(RxState.WaitPid){
                when(io.utmi_rx_error){
                    // FIXME: Do anything else?
                    rx_state        := RxState.WaitPacketDone
                }
                .elsewhen(!io.utmi_rx_active){
                    // FIXME: This should never happen. Return a trans_err instead?
                    pkt_end         := True
                    pkt_active      := False

                    rx_state        := RxState.Idle
                }
                .elsewhen(io.utmi_rx_valid && !pid_valid){
                    // This will generate a trans_err when the packet is done.
                    rx_state        := RxState.WaitPacketDone
                }
                .elsewhen(io.utmi_rx_valid){
                    cur_pkt_pid     := pid_value

                    switch(pid_value){
                        is(PidType.SOF.asBits, PidType.IN.asBits, PidType.OUT.asBits, PidType.SETUP.asBits){
                            u_crc5.io.flush     := True
                            rx_state            := RxState.WaitToken1
                        }
                        is(PidType.DATA0.asBits, PidType.DATA1.asBits){
                        }
                    }
                }
            }

            //============================================================
            // Receive DATA0/DATA1
            //============================================================
            is(RxState.WaitData){
                when(io.utmi_rx_error){
                    // Generate a trans_err when packet is done
                    cur_pkt_pid     := PidType.NULL.asBits
                }
                .elsewhen(!io.utmi_rx_active){
                    rx_state        := RxState.Idle
                }
                .elsewhen(io.utmi_rx_valid){
                    io.pkt_data_valid       := True
                    io.pkt_data             := io.utmi_rx_data
                }
            }

            //============================================================
            // TOKEN
            //============================================================
            is(RxState.WaitToken1){
                when(io.utmi_rx_error){
                    // Generate a trans_err when packet is done
                    cur_pkt_pid     := PidType.NULL.asBits
                    rx_state        := RxState.WaitPacketDone
                }
                .elsewhen(!io.utmi_rx_active){
                    // Token aborted. Generate trans_err
                    cur_pkt_pid     := PidType.NULL.asBits
                    rx_state        := RxState.WaitPacketDone
                }
                .elsewhen(io.utmi_rx_valid){
                    cur_pkt_token_data(7 downto 0)  := io.utmi_rx_data

                    rx_state        := RxState.WaitToken2
                }
            }
            is(RxState.WaitToken2){
                when(io.utmi_rx_error){
                    // Generate a trans_err when packet is done
                    cur_pkt_pid     := PidType.NULL.asBits
                    rx_state        := RxState.WaitPacketDone
                }
                .elsewhen(!io.utmi_rx_active){
                    cur_pkt_pid     := PidType.NULL.asBits
                    rx_state        := RxState.WaitPacketDone
                }
                .elsewhen(io.utmi_rx_valid){
                    cur_pkt_token_data(10 downto 8) := io.utmi_rx_data(2 downto 0)
                    cur_pkt_crc5                    := io.utmi_rx_data(7 downto 3)  

                    u_crc5.io.input.valid       := True
                    u_crc5.io.input.payload     := io.utmi_rx_data(2 downto 0) ## cur_pkt_token_data(7 downto 0)

                    rx_state        := RxState.WaitPacketDone
                }
            }
            is(RxState.WaitPacketDone){
                when(io.utmi_rx_error){
                    // trans_err
                    cur_pkt_pid     := PidType.NULL.asBits
                }
                .elsewhen(io.utmi_rx_valid){
                  // Unexpected data?! -> trans_err
                    cur_pkt_pid     := PidType.NULL.asBits
                }
                .elsewhen(!io.utmi_rx_active){
                    pkt_end         := True
                    pkt_active      := False
                    pkt_trans_err   := (cur_pkt_pid === PidType.NULL.asBits) || !token_crc5_match

                    io.pkt_is_token := ((cur_pkt_pid === PidType.SETUP.asBits) || 
                                       (cur_pkt_pid === PidType.IN.asBits)     || 
                                       (cur_pkt_pid === PidType.OUT.asBits))   && token_crc5_match

                    io.pkt_is_sof   := (cur_pkt_pid === PidType.SOF.asBits)    && token_crc5_match

                    rx_state        := RxState.Idle
                }
            }
        }

    }

}



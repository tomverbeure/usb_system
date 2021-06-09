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

        // Probably not needed outside this FSM...
        val pkt_start             = out(Bool)
        val pkt_end               = out(Bool)
        val pkt_active            = out(Bool)


        // True for 1 cycle when packet information must be transferred.
        // * TOKEN packet (SETUP, IN, OUT, SOF): asserted at the end of a packet,  
        //   the PID is not corrupt, and the CRC5 matches.
        // * DATA0/DATA1/DATA2/DATAM packet: asserted as soon as the non-corrupt PID
        //   is received. (IOW: don't wait until the end of the packet.
        // * Everything else (handshake etc): asserted at the end of the packet.
        //   These packets consist of a PID only, so it gets asserted when the PID is not
        //   not corrupt.

        val pkt_valid             = out(Bool)

        // trans_err can happen when the lower PID nibble doesn't match the inverted upper 
        // PID nibble, or when crc5 of a token packet is incorrect.
        // Valid for 1 clock cycle. pkt_valid is not asserted in this case.

        val pkt_trans_err         = out(Bool)

        // Asserted, and only valid, when pkt_valid is asserted.
        val pkt_pid               = out(Bits(4 bits))

        // Asserted when pkt_pid is SETUP, IN, or OUT. 
        // Only valid when pkt_valid is true.
        val pkt_is_token          = out(Bool)
        val pkt_addr              = out(UInt(7 bits))
        val pkt_endp              = out(UInt(4 bits))

        // Asserted when pkt_pid is SOF.
        // Only valid when pkt_valid is true.
        val pkt_is_sof            = out(Bool)
        val pkt_frame_nr          = out(UInt(11 bits))

        // Asserted when pkt_pid is DATA0/DATA1/DATAM/DATAX.
        // Only valid when pkt_valid is true.
        val pkt_is_data           = out(Bool)

        // Asserted when a new data byte has been received while inside
        // a DATA packet.
        // This means that it will be asserted after a pkt_is_data event, but
        // before pkt_data_done.
        val pkt_data_valid        = out(Bool)
        val pkt_data              = out(Bits(8 bits))

        // Asserted at the end of a received data packet.
        // When the crc16 matches, pkt_data_err stays low. Otherwise,
        // pkt_data_err goes high together with pkt_data_one.
        val pkt_data_done         = out(Bool)
        val pkt_data_err          = out(Bool)

        // Asserted when there has been a crc5 or cr16 mismatch.
        // This is only to be used for error counters and such.
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
            val WaitTokenPacketDone     = newElement()
            val WaitDataPacketDone      = newElement()
            val WaitOtherPacketDone     = newElement()
        }

        val u_crc5 = Crc(CrcKind.usb.crc5, 8)
        u_crc5.io.input.valid   := False
        u_crc5.io.input.payload := io.utmi_rx_data
        u_crc5.io.flush         := False

        val token_crc5_match    = u_crc5.io.result === B(0x19, 5 bits)

        val u_crc16 = Crc(CrcKind.usb.crc16, 8)
        u_crc16.io.input.valid   := False
        u_crc16.io.input.payload := io.utmi_rx_data
        u_crc16.io.flush         := False

        val data_crc16_match    = u_crc16.io.result === B(0x4ffe, 16 bits)

        val rx_state = Reg(RxState()) init(RxState.Idle)


        val pkt_start     = Bool
        val pkt_end       = Bool
        val pkt_active    = Reg(Bool) init(False)
        val pkt_valid     = Bool
        val pkt_trans_err = Bool
        val pkt_data_done = Bool
        val pkt_data_err  = Bool

        io.pkt_start      := pkt_start
        io.pkt_end        := pkt_end
        io.pkt_active     := pkt_active
        io.pkt_valid      := pkt_valid
        io.pkt_trans_err  := pkt_trans_err
        io.pkt_data_done  := pkt_data_done
        io.pkt_data_err   := pkt_data_err

        pkt_start       := False
        pkt_end         := False
        pkt_valid       := False
        pkt_trans_err   := False
        pkt_data_done   := False
        pkt_data_err    := False

        val rx_error_seen = Reg(Bool) init(False)

        switch(rx_state){
            is(RxState.Idle){
                when(io.utmi_rx_active){
                    pkt_start       := True
                    pkt_active      := True
                    cur_pkt_pid     := PidType.NULL.asBits
                    rx_error_seen   := False

                    rx_state        := RxState.WaitPid
                }
            }

            is(RxState.WaitPid){
                when(io.utmi_rx_error){
                    rx_error_seen   := True
                    rx_state        := RxState.WaitOtherPacketDone
                }
                .elsewhen(!io.utmi_rx_active){
                    // FIXME: This should never happen. Return a trans_err instead?
                    pkt_end         := True
                    pkt_active      := False

                    rx_state        := RxState.Idle
                }
                .elsewhen(io.utmi_rx_valid && !pid_valid){
                    // This will generate a trans_err when the packet is done.
                    rx_error_seen   := True
                    rx_state        := RxState.WaitOtherPacketDone
                }
                .elsewhen(io.utmi_rx_valid){
                    cur_pkt_pid     := pid_value

                    switch(pid_value){
                        is(PidType.SOF.asBits, PidType.IN.asBits, PidType.OUT.asBits, PidType.SETUP.asBits){
                            u_crc5.io.flush     := True
                            rx_state            := RxState.WaitToken1
                        }
                        is(PidType.DATA0.asBits, PidType.DATA1.asBits){
                            u_crc16.io.flush    := True
                            pkt_valid           := True
                            io.pkt_pid          := pid_value            // Override default assignment from FF.
                            io.pkt_is_data      := True
                            rx_state            := RxState.WaitData
                        }
                    }
                }
            }
            //============================================================
            // TOKEN
            //============================================================
            is(RxState.WaitToken1){
                when(io.utmi_rx_error){
                    // Generate a trans_err when packet is done
                    rx_error_seen   := True
                    rx_state        := RxState.WaitOtherPacketDone
                }
                .elsewhen(!io.utmi_rx_active){
                    // Another byte expected, not end of the packet.
                    rx_error_seen   := True
                    rx_state        := RxState.WaitOtherPacketDone
                }
                .elsewhen(io.utmi_rx_valid){
                    cur_pkt_token_data(7 downto 0)  := io.utmi_rx_data
                    u_crc5.io.input.valid           := True

                    rx_state        := RxState.WaitToken2
                }
            }
            is(RxState.WaitToken2){
                when(io.utmi_rx_error){
                    // Generate a trans_err when packet is done
                    rx_error_seen   := True
                    rx_state        := RxState.WaitOtherPacketDone
                }
                .elsewhen(!io.utmi_rx_active){
                    // Another byte expected, not end of the packet.
                    rx_error_seen   := True
                    rx_state        := RxState.WaitOtherPacketDone
                }
                .elsewhen(io.utmi_rx_valid){
                    cur_pkt_token_data(10 downto 8) := io.utmi_rx_data(2 downto 0)
                    u_crc5.io.input.valid           := True

                    rx_state        := RxState.WaitTokenPacketDone
                }
            }
            is(RxState.WaitTokenPacketDone){
                when(io.utmi_rx_error){
                    // trans_err
                    rx_error_seen   := True
                    rx_state        := RxState.WaitOtherPacketDone
                }
                .elsewhen(io.utmi_rx_valid){
                    // Unexpected data?! -> trans_err
                    rx_error_seen   := True
                    rx_state        := RxState.WaitOtherPacketDone
                }
                .elsewhen(!io.utmi_rx_active){
                    pkt_end         := True
                    pkt_active      := False
                    pkt_valid       := token_crc5_match
                    pkt_trans_err   := !token_crc5_match
                    io.pkt_is_token := (cur_pkt_pid =/= PidType.SOF.asBits)
                    io.pkt_is_sof   := (cur_pkt_pid === PidType.SOF.asBits)

                    rx_state        := RxState.Idle
                }
            }
            //============================================================
            // Receive DATA0/DATA1
            //============================================================
            is(RxState.WaitData){
                when(io.utmi_rx_error){
                    // Generate a pkt_data_err when packet is done
                    rx_error_seen   := True
                    rx_state        := RxState.WaitDataPacketDone
                }
                .elsewhen(!io.utmi_rx_active){
                    rx_state        := RxState.WaitDataPacketDone
                }
                .elsewhen(io.utmi_rx_valid){
                    io.pkt_data_valid       := True
                    u_crc16.io.input.valid  := True
                }
            }
            is(RxState.WaitDataPacketDone){
                when(io.utmi_rx_error){
                    rx_error_seen   := True
                }
                .elsewhen(io.utmi_rx_valid){
                    // This should never happen, because we can only get
                    // in this state when rx_active become 0 or after an rx_error.
                    rx_error_seen   := True
                }
                .elsewhen(!io.utmi_rx_active){
                    pkt_end         := True
                    pkt_active      := False
                    pkt_data_done   := True
                    pkt_data_err    := rx_error_seen || !data_crc16_match

                    rx_state        := RxState.Idle
                }
            }
            //============================================================
            // Receive HANDSHAKE or an incorrect other packet
            //============================================================
            is(RxState.WaitOtherPacketDone){
                when(io.utmi_rx_error){
                    rx_error_seen   := True
                }
                .elsewhen(io.utmi_rx_valid){
                    rx_error_seen   := True
                }
                .elsewhen(!io.utmi_rx_active){
                    pkt_end         := True
                    pkt_active      := False
                    pkt_trans_err   := rx_error_seen 

                    rx_state        := RxState.Idle
                }
            }
        }

    }

}



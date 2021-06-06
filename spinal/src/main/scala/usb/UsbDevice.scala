package usb

import spinal.core._
import spinal.lib._
import spinal.lib.Reverse
import spinal.lib.io._
import spinal.lib.bus.simple._
import spinal.lib.bus.misc._
import spinal.lib.bus.amba3.apb._
import spinal.lib.com.eth.{Crc, CrcKind}

object UsbDevice {
    def getApb3Config() = Apb3Config(addressWidth = 13, dataWidth=32)

    object PidType extends SpinalEnum {
        val NULL                        = newElement()
        val OUT, IN, SOF, SETUP         = newElement()
        val DATA0, DATA1, DATA2, MDATA  = newElement()
        val ACK, NAK, STALL, NYET       = newElement()
        val PRE_ERR, SPLIT, PING        = newElement()

        defaultEncoding = SpinalEnumEncoding("staticEncoding")(
            NULL        -> 0x0,
            // Tokens
            OUT         -> 0x1,
            IN          -> 0x9,
            SOF         -> 0x5,
            SETUP       -> 0xd,
            // Data
            DATA0       -> 0x3,
            DATA1       -> 0xb,
            DATA2       -> 0x7,
            MDATA       -> 0xf,
            // Handshake
            ACK         -> 0x2,
            NAK         -> 0xa,
            STALL       -> 0xe,
            NYET        -> 0x6,
            // Special
            PRE_ERR     -> 0xc,
            SPLIT       -> 0x8,
            PING        -> 0x4
        )
    }


    // Time to go from FS normal mode to suspend
    val t_fs_suspend        = 3 ms
    val t_fs_suspend_sim    = 30 us

    // Time to wait after start of SE0 to start HS detection handshake
    val t_wtrstfs           = 1.5 ms
    val t_wtrstfs_sim       = 15 us

    // Time to wait after start of SE0 to revertfrom HS to FS
    val t_wtrev             = 1.5 ms
    val t_wtrev_sim         = 15 us

    // Time to wait after reverting from HS to FS before sampling the bus to check for SE0
    val t_wtrsths           = 400 us
    val t_wtrsths_sim       = 4 us

    // Time for a device to come out of suspend mode and get all its internal clocks running
    // Implementation dependent and not a fixed value defined in the spec.
    // "Should have a value somewhere between 0 and 5ms."
    // See USB C.2.1.
    // FIXME: instead of being hard-coded, can we get this from the UTMI block?
    val t_wtclk             = 2 ms
    val t_wtclk_sim         = 20 us

    // Minimum Chirp K from a HS capable device within reset protocol
    val t_uch               = 1 ms
    val t_uch_sim           = 10 us

    // Time after start of SE0 by which a HS device is required to have completed its Chirp K
    // within the reset protocol
    val t_uchend            = 7 ms
    val t_uchend_sim        = 70 us

    // Time for which as suspended HS device must see SE0 before beginning a HS detection
    // handshake
    val t_filtse0           = 2.5 us
    val t_filtse0_sim       = 2.5 us

    // Time for which a Chirp J or Chirp K must be continuously detected by device during
    // reset handshake
    val t_filt              = 2.5 us
    val t_filt_sim          = 2.5 us

    // Time after end of upstream chirp at which device reverts to FS default state
    // is no downstream chirp is detected
    val t_wtfs              = 2.5 ms
    val t_wtfs_sim          = 25 us
}

case class UsbDevice(isSim : Boolean = false) extends Component {

    import UsbDevice._
    import Utmi._

    // Everything in this block runs at ULPI 60MHz clock speed.
    // If the APB is running at a different clock speed, use Apb3CC which is a clock crossing
    // APB bridge.

    val io = new Bundle {
        val utmi                          = slave(Utmi())

        val sof_frame_nr                  = out(UInt(11 bits))

        val rx_valid                      = out(Bool)
        val rx_data                       = out(Bits(8 bits))

        val line_state                    = out(Bits(2 bits))

        val enable_hs                     = in(Bool)

        // Debug options
        val force_suspend_m               = in(Bool)
        val force_suspend_m_value         = in(Bool)

        val force_xcvr_select             = in(Bool)
        val force_xcvr_select_value       = in(Bits(2 bits))

        val force_term_select             = in(Bool)
        val force_term_select_value       = in(Bool)

        val force_op_mode                 = in(Bool)
        val force_op_mode_value           = in(Bits(2 bits))

    }

    val utmi_tx_valid     = Bool
    val utmi_tx_data      = Bits(8 bits)

    io.utmi.tx_valid      := utmi_tx_valid
    io.utmi.data_in       := utmi_tx_data

    val utmi_xcvr_select  = Reg(Bits(2 bits)) init(Utmi.XcvrSelect.FS)
    val utmi_term_select  = Reg(Bool) init(True)
    val utmi_suspend_m    = Reg(Bool) init(True)
    val utmi_op_mode      = Reg(Bits(2 bits)) init(0)

    io.utmi.xcvr_select   := utmi_xcvr_select
    io.utmi.term_select   := utmi_term_select
    io.utmi.suspend_m     := utmi_suspend_m
    io.utmi.op_mode       := utmi_op_mode

    // For now, always select FS transceivers enabled
    utmi_xcvr_select      := io.force_xcvr_select ? io.force_xcvr_select_value | B(Utmi.XcvrSelect.FS, 2 bits)

    // When in FS mode, enable 1.5K pullup on DP
    utmi_term_select      := io.force_term_select ? io.force_term_select_value | True

    // For now, always keep UTMI cell powered`
    utmi_suspend_m        := io.force_suspend_m ? io.force_suspend_m_value | True

    utmi_op_mode          := B(Utmi.OpMode.Normal, 2 bits)

    val go_to_suspended         = Bool
    val resume_from_suspended   = Bool
    val go_to_default           = Bool
    val address_assigned        = Bool
    val device_configured       = Bool
    val device_deconfigured     = Bool

    val cur_speed_is_fs         = Reg(Bool) init(True)
    val enable_transactions     = Bool
    val enable_rx_packets       = Bool

    address_assigned            := False
    device_configured           := False
    device_deconfigured         := False

    val clock_speed       = 60 MHz

    val cyc_fs_suspend  = ((if (isSim) t_fs_suspend_sim  else t_fs_suspend) * clock_speed).toInt
    val cyc_wtrstfs     = ((if (isSim) t_wtrstfs_sim     else t_wtrstfs)    * clock_speed).toInt
    val cyc_wtrev       = ((if (isSim) t_wtrev_sim       else t_wtrev)      * clock_speed).toInt
    val cyc_wtrsths     = ((if (isSim) t_wtrsths_sim     else t_wtrsths)    * clock_speed).toInt
    val cyc_wtclk       = ((if (isSim) t_wtclk_sim       else t_wtclk)      * clock_speed).toInt
    val cyc_uch         = ((if (isSim) t_uch_sim         else t_uch)        * clock_speed).toInt
    val cyc_uchend      = ((if (isSim) t_uchend_sim      else t_uchend)     * clock_speed).toInt
    val cyc_filtse0     = ((if (isSim) t_filtse0_sim     else t_filtse0)    * clock_speed).toInt
    val cyc_filt        = ((if (isSim) t_filt_sim        else t_filt)       * clock_speed).toInt
    val cyc_wtfs        = ((if (isSim) t_wtfs_sim        else t_wtfs)       * clock_speed).toInt


    val device_fsm = new Area {

        enable_transactions     := False

        // USB 2.0, section 9.1.1: Visible Device States

        object DeviceState extends SpinalEnum {
            // There is no way to know the difference between detached and
            // attached.
            val Attached              = newElement()
            val Powered               = newElement()
            val Default               = newElement()
            val Address               = newElement()
            val Configured            = newElement()
            val Suspended             = newElement()
        }

        val dev_state           = Reg(DeviceState()) init(DeviceState.Attached)
        val pre_suspend_state   = Reg(DeviceState()) init(DeviceState.Attached)
        
        switch(dev_state){
            //============================================================
            // ATTACHED
            //============================================================
            is(DeviceState.Attached){
                //when(io.utmi.vbus_valid){
                when(True){
                    dev_state         := DeviceState.Powered
                    cur_speed_is_fs   := True
                }
            }

            //============================================================
            // POWERED
            //============================================================
            is(DeviceState.Powered){
                when(go_to_suspended){
                    pre_suspend_state := dev_state
                    dev_state         := DeviceState.Suspended
                }
                .elsewhen(go_to_default){
                    dev_state         := DeviceState.Default
                }
            }

            //============================================================
            // DEFAULT
            //============================================================
            is(DeviceState.Default){
                enable_transactions   := True

                when(go_to_suspended){
                    pre_suspend_state := dev_state
                    dev_state         := DeviceState.Suspended
                }
                .elsewhen(address_assigned){
                    dev_state         := DeviceState.Address
                }
            }

            //============================================================
            // ADDRESS ASSIGNED
            //============================================================
            is(DeviceState.Address){
                enable_transactions   := True

                when(go_to_suspended){
                    pre_suspend_state := dev_state
                    dev_state         := DeviceState.Suspended
                }
                .elsewhen(device_configured){
                    dev_state         := DeviceState.Configured
                }
            }

            //============================================================
            // CONFIGURED
            //============================================================
            is(DeviceState.Configured){
                enable_transactions   := True

                when(go_to_suspended){
                    pre_suspend_state := dev_state
                    dev_state         := DeviceState.Suspended
                }
                .elsewhen(device_deconfigured){
                    dev_state         := DeviceState.Address
                }
            }

            //============================================================
            // SUSPENDED
            //============================================================
            is(DeviceState.Suspended){
                when(resume_from_suspended){
                    dev_state         := pre_suspend_state
                }
            }

        }

        val normal_state   = (    dev_state === DeviceState.Powered 
                               || dev_state === DeviceState.Default 
                               || dev_state === DeviceState.Address 
                               || dev_state === DeviceState.Configured) 

        val fs_normal_state   = normal_state &&  cur_speed_is_fs
        val hs_normal_state   = normal_state && !cur_speed_is_fs
        val suspended_state   = dev_state === DeviceState.Suspended
    }

    //============================================================
    // Reset FSM
    //============================================================
    
    val fs_idle_detected  = io.utmi.line_state === Utmi.LineState.J
    val hs_idle_detected  = io.utmi.line_state === Utmi.LineState.SE0
    val se0_detected      = io.utmi.line_state === Utmi.LineState.SE0
    val k_detected        = io.utmi.line_state === Utmi.LineState.K
    val chirp_j_detected  = io.utmi.line_state === Utmi.LineState.J
    val chirp_k_detected  = io.utmi.line_state === Utmi.LineState.K

    val line_state_prev     = RegNext(io.utmi.line_state) init(Utmi.LineState.SE0)
    val line_state_changed  = io.utmi.line_state =/= line_state_prev


    val reset_fsm = new Area {
        // USB 2.0: Appendix C.2: Upstream Facing Port State Diagram

        val t0_cntr     = Reg(UInt(24 bits)) init(0)
        val t1_cntr     = Reg(UInt(24 bits)) init(0)
        val t2_cntr     = Reg(UInt(24 bits)) init(0)
        val c0_cntr     = Reg(UInt( 3 bits)) init(0)

        object ResetState extends SpinalEnum {
            // There is no way to know the difference between detached and
            // attached.
            val Idle                  = newElement()

            val FsSe0OrIdle           = newElement()
            val HsIdle                = newElement()
            val HsDecideRstSus        = newElement()
            val WaitClkStartup        = newElement()
            val WaitSe0Detect         = newElement()
            val CheckSe0Duration      = newElement()
            val SusSe0Recheck         = newElement()

            val StartChirpK           = newElement()
            val DriveChirpK            = newElement()
            val WaitChirpK            = newElement()
            val CheckChirpKDuration   = newElement()
            val WaitChirpJ            = newElement()
            val CheckChirpJDuration   = newElement()
            val CheckC0               = newElement()
            
            val FsDefault             = newElement()
            val HsDefault             = newElement()
        }

        val reset_state =  Reg(ResetState()) init(ResetState.Idle)

        go_to_suspended         := False
        go_to_default           := False
        resume_from_suspended   := False

        t0_cntr   :=  t0_cntr + 1
        t1_cntr   :=  t1_cntr + 1

        switch(reset_state){
            is(ResetState.Idle){
                t0_cntr   := 0
                t1_cntr   := 0

                // C.2.1: Reset from Suspended State
                when(device_fsm.suspended_state && se0_detected){
                    reset_state   := ResetState.WaitClkStartup
                }
                .elsewhen(device_fsm.suspended_state && k_detected){
                    resume_from_suspended   := True
                }
                // C.2.2: Reset From FS Non-Suspended State
                .elsewhen(device_fsm.fs_normal_state && (se0_detected || fs_idle_detected)){
                    reset_state   := ResetState.FsSe0OrIdle
                }
                // C.2.3 Reset from HS Non-suspended State
                .elsewhen(device_fsm.hs_normal_state && hs_idle_detected){
                    reset_state   := ResetState.HsIdle
                }
            }

            is(ResetState.FsSe0OrIdle){
                when(line_state_changed){
                    reset_state             := ResetState.Idle
                }
                .elsewhen(fs_idle_detected && t0_cntr >= cyc_fs_suspend){
                    go_to_suspended         := True
                    reset_state             := ResetState.Idle
                }
                .elsewhen(se0_detected && t0_cntr >= cyc_wtrstfs){
                    reset_state             := ResetState.StartChirpK
                }
            }

            is(ResetState.HsIdle){
                when(line_state_changed){
                    reset_state             := ResetState.Idle
                }
                .elsewhen(t0_cntr >= cyc_wtrev){
                    // FIXME: remove HS terminations, connect FS pullup
                    t1_cntr                 := 0
                    reset_state             := ResetState.HsDecideRstSus
                }
            }

            is(ResetState.HsDecideRstSus){
                when(t1_cntr >= cyc_wtrsths){
                    when(se0_detected){
                        reset_state         := ResetState.StartChirpK
                    }
                    .otherwise{
                        go_to_suspended       := True
                        reset_state           := ResetState.Idle
                    }
                }
            }

            // Wait for the clocks in the UTM to power up and become stable
            is(ResetState.WaitClkStartup){
                when(t0_cntr >= cyc_wtclk){
                    t0_cntr                   := 0
                    reset_state               := ResetState.WaitSe0Detect
                }
            }

            is(ResetState.WaitSe0Detect){
                t1_cntr     := 0

                when(se0_detected){
                    reset_state               := ResetState.CheckSe0Duration
                }
                .elsewhen(t0_cntr >= (cyc_uchend - cyc_uch)){
                    reset_state               := ResetState.Idle
                }
            }


            is(ResetState.CheckSe0Duration){

                when(!se0_detected && (t1_cntr < cyc_filtse0)){
                    reset_state               := ResetState.WaitSe0Detect
                }
                .elsewhen(t0_cntr >= (cyc_uchend - cyc_uch) && (t1_cntr < cyc_filtse0)){
                    reset_state               := ResetState.Idle
                }
                .elsewhen(t1_cntr >= cyc_filtse0){
                    reset_state               := ResetState.StartChirpK
                }

            }

            is(ResetState.StartChirpK){
                // FIXME: drive ChirpK

                t0_cntr     := 0          // timer T2 is mapped to timer T0
                reset_state                 := ResetState.DriveChirpK
            }

            is(ResetState.DriveChirpK){
                when(t0_cntr >= cyc_uch){
                    // FIXME: stop driving ChirpK
                    c0_cntr     := 0
                    t0_cntr     := 0      // timer T3 is mapped to timer T0

                    reset_state             := ResetState.WaitChirpK
                }
            }

            is(ResetState.WaitChirpK){
                t1_cntr         := 0      // timer T4 is mapped to timer T1

                when(t0_cntr >= cyc_wtfs){
                    reset_state             := ResetState.FsDefault
                }
                .elsewhen(chirp_k_detected){
                    reset_state             := ResetState.CheckChirpKDuration
                }
            }

            is(ResetState.CheckChirpKDuration){
                when(t0_cntr >= cyc_wtfs){
                    reset_state             := ResetState.FsDefault
                }
                .elsewhen(!chirp_k_detected && t1_cntr < cyc_filt){
                    reset_state             := ResetState.WaitChirpK
                }
                .elsewhen(t1_cntr >= cyc_filt){
                    reset_state             := ResetState.WaitChirpJ
                }
            }

            is(ResetState.WaitChirpJ){
                t1_cntr         := 0      // timer T4 is mapped to timer T1

                when(t0_cntr >= cyc_wtfs){
                    reset_state             := ResetState.FsDefault
                }
                .elsewhen(chirp_j_detected){
                    reset_state             := ResetState.CheckChirpJDuration
                }
            }

            is(ResetState.CheckChirpJDuration){
                when(t0_cntr >= cyc_wtfs){
                    reset_state             := ResetState.FsDefault
                }
                .elsewhen(!chirp_k_detected && t1_cntr < cyc_filt){
                    reset_state             := ResetState.WaitChirpJ
                }
                .elsewhen(t1_cntr >= cyc_filt){
                    c0_cntr                 := c0_cntr + 1
                    reset_state             := ResetState.CheckC0
                }
            }

            is(ResetState.CheckC0){
                when(t0_cntr >= cyc_wtfs){
                    reset_state             := ResetState.FsDefault
                }
                .elsewhen(c0_cntr < 3){
                    reset_state             := ResetState.WaitChirpK
                }
                .otherwise{
                    reset_state             := ResetState.HsDefault
                }
            }

            is(ResetState.FsDefault){
                go_to_default     := True
                cur_speed_is_fs   := True

                reset_state       := ResetState.Idle
            }

            is(ResetState.HsDefault){
                // FIXME: Enable HS terminations, disconnect FS pullup
                go_to_default     := True
                cur_speed_is_fs   := False

                reset_state       := ResetState.Idle
            }
        }
    }
    
    //============================================================
    // Transaction FSM
    //============================================================

    val transaction_fsm = new Area {

        object TransState extends SpinalEnum {
            val Idle                  = newElement()
            val GetPid                = newElement()
            val SetupSendData0        = newElement()
            val SetupWaitHandshake    = newElement()
        }

        utmi_tx_valid     := False
        utmi_tx_data      := 0

        val trans_state = Reg(TransState()) init(TransState.Idle)

        when(enable_transactions){
    
            switch(trans_state){
                //============================================================
                // IDLE
                //============================================================
                is(TransState.Idle){
                }
            }
        }
        .otherwise{
            trans_state     := TransState.Idle
        }
    }

    //============================================================
    // Rx FSM
    //============================================================

    val rx_fsm = new Area {

        val pid_value       =  io.utmi.data_out(3 downto 0)
        val pid_value_alt   = ~io.utmi.data_out(7 downto 4)
        val pid_valid       = pid_value === pid_value_alt

        val cur_frame_nr    = Reg(UInt(11 bits)) init(0)
        val cur_crc5        = Reg(Bits(5 bits)) init(0)

        val sof_frame_nr    = Reg(UInt(11 bits)) init(0)
        io.sof_frame_nr     := sof_frame_nr

        val sof_crc5_err_cnt    = Reg(UInt(8 bits)) init(0)

        object RxState extends SpinalEnum {
            val Idle                    = newElement()
            val WaitPid                 = newElement()
            val WaitSof1                = newElement()
            val WaitSof2                = newElement()
            val SofCrcCheck             = newElement()
            val ErrorWaitPacketDone     = newElement()
        }

        val u_crc5 = Crc(CrcKind.usb.crc5, 11)
        u_crc5.io.input.valid   := False
        u_crc5.io.input.payload := 0
        u_crc5.io.flush         := False

        val rx_state = Reg(RxState()) init(RxState.Idle)

        io.rx_valid   := False
        io.rx_data    := 0

        val sof_crc5_calc = u_crc5.io.result
        val sof_crc5_match = sof_crc5_calc === cur_crc5

        switch(rx_state){
            is(RxState.Idle){
                when(io.utmi.rx_active){
                    rx_state    := RxState.WaitPid
                }
            }

            is(RxState.WaitPid){
                // FIXME: deal with error handling
                when(!io.utmi.rx_active){
                    rx_state    := RxState.Idle
                }
                .elsewhen(io.utmi.rx_valid && !pid_valid){
                    rx_state    := RxState.ErrorWaitPacketDone
                }
                .elsewhen(io.utmi.rx_valid){
                    io.rx_valid     := io.utmi.rx_valid
                    io.rx_data      := io.utmi.data_out

                    switch(pid_value){
                        is(PidType.SOF.asBits){
                            u_crc5.io.flush     := True
                            rx_state            := RxState.WaitSof1
                        }
                    }
                    
                }
            }
            is(RxState.WaitSof1){
                when(!io.utmi.rx_active){
                    rx_state        := RxState.Idle
                }
                .elsewhen(io.utmi.rx_valid){
                    cur_frame_nr(7 downto 0)    := io.utmi.data_out.asUInt

                    rx_state        := RxState.WaitSof2
                }
            }
            is(RxState.WaitSof2){
                when(!io.utmi.rx_active){
                    rx_state        := RxState.Idle
                }
                .elsewhen(io.utmi.rx_valid){
                    cur_frame_nr(10 downto 8)   := io.utmi.data_out(2 downto 0).asUInt

                    cur_crc5                    := io.utmi.data_out(7 downto 3)  

                    u_crc5.io.input.valid       := True
                    u_crc5.io.input.payload     := io.utmi.data_out(2 downto 0) ## cur_frame_nr(7 downto 0).asBits

                    rx_state        := RxState.SofCrcCheck
                }
            }
            is(RxState.SofCrcCheck){
                when(sof_crc5_match){
                    sof_frame_nr    := cur_frame_nr
                    rx_state        := RxState.Idle
                }
                .otherwise{
                    sof_crc5_err_cnt    := sof_crc5_err_cnt + 1
                    rx_state        := RxState.Idle
                }
            }
            is(RxState.ErrorWaitPacketDone){
                when(!io.utmi.rx_active){
                    rx_state        := RxState.Idle
                }
            }
        }

    }

    //============================================================
    // Registers
    //============================================================

    def driveFrom(busCtrl: BusSlaveFactory, baseAddress: BigInt) = new Area {
        
      val rx_data_valid = busCtrl.createReadOnly(io.rx_valid,       0x100, 0)
      val rx_data       = busCtrl.createReadOnly(io.rx_data,        0x100, 8)
        
      rx_data_valid   := io.rx_valid.addTag(crossClockDomain)
      rx_data         := io.rx_data.addTag(crossClockDomain)

      //============================================================
      // CONFIG 
      //============================================================

      val enable_hs                 = busCtrl.createReadAndWrite(Bool,              0x00, 0) init(False)

      io.enable_hs                  := BufferCC(enable_hs)

      //============================================================
      // STATUS 
      //============================================================

      val sof_frame_nr              = busCtrl.createReadOnly(io.sof_frame_nr,       0x04, 0)

      sof_frame_nr      := io.sof_frame_nr.addTag(crossClockDomain)


      //============================================================
      // DEBUG 
      //============================================================
      val force_term_select        = busCtrl.createReadAndWrite(Bool,               0x10, 0) init(False)
      val force_term_select_value  = busCtrl.createReadAndWrite(Bool,               0x10, 1) init(False)

      val force_suspend_m          = busCtrl.createReadAndWrite(Bool,               0x10, 2) init(False)
      val force_suspend_m_value    = busCtrl.createReadAndWrite(Bool,               0x10, 3) init(True)

      val force_xcvr_select        = busCtrl.createReadAndWrite(Bool,               0x10, 4) init(False)
      val force_xcvr_select_value  = busCtrl.createReadAndWrite(Bits(2 bits),       0x10, 5) init(0)

      val force_op_mode            = busCtrl.createReadAndWrite(Bool,               0x10, 7) init(False)
      val force_op_mode_value      = busCtrl.createReadAndWrite(Bits(2 bits),       0x10, 8) init(0)

      io.force_term_select          := force_term_select.addTag(crossClockDomain)
      io.force_term_select_value    := force_term_select_value.addTag(crossClockDomain)
      io.force_suspend_m            := force_suspend_m.addTag(crossClockDomain)
      io.force_suspend_m_value      := force_suspend_m_value.addTag(crossClockDomain)
      io.force_xcvr_select          := force_xcvr_select.addTag(crossClockDomain)
      io.force_xcvr_select_value    := force_xcvr_select_value.addTag(crossClockDomain)
      io.force_op_mode              := force_op_mode.addTag(crossClockDomain)
      io.force_op_mode_value        := force_op_mode_value.addTag(crossClockDomain)
    }

}


case class UsbDeviceTop() extends Component
{
    val io = new Bundle {
        val apb         = slave(Apb3(UsbDevice.getApb3Config()))
    }

    val u_usb_device  = UsbDevice()
    val busCtrl       = Apb3SlaveFactory(io.apb)
    val apb_regs      = u_usb_device.driveFrom(busCtrl, 0x0)

}


object UsbDeviceVerilog{
    def main(args: Array[String]) {

        val config = SpinalConfig(anonymSignalUniqueness = true)
        config.includeFormal.generateSystemVerilog({
            val toplevel = new UsbDeviceTop()
            toplevel
        })
        println("DONE")
    }
}


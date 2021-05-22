
package top

import spinal.core._
import spinal.lib._
import spinal.lib.io._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc.SizeMapping
import spinal.lib.com.uart._
import spinal.lib.com.jtag.Jtag

import scala.collection.mutable.ArrayBuffer
import cc._
import usb._

case class CpuTop(hasJtagUart : Boolean, hasUart : Boolean) extends Component {

    val io = new Bundle {
        val led_red         = out(Bool)
        val led_green       = out(Bool)
        val led_blue        = out(Bool)

        val uart            = if (hasUart) master(Uart()) else null

        val usb_dev_apb     = master(Apb3(UsbDevice.getApb3Config()))

        val jtag            = slave(Jtag())
    }

    val cpuConfig = CpuComplexConfig.default.copy(onChipRamBinFile = "../sw/progmem4k.bin")
    //val cpuConfig = CpuComplexConfig.default

    val u_cpu = CpuComplex(cpuConfig)
    u_cpu.io.jtag           <> io.jtag
    u_cpu.io.externalInterrupt      := False

    val apbMapping = ArrayBuffer[(Apb3, SizeMapping)]()

    //============================================================
    // Timer
    //============================================================

    val u_timer = new CCApb3Timer()
    u_timer.io.interrupt        <> u_cpu.io.timerInterrupt
    apbMapping += u_timer.io.apb -> (0x00000, 4 kB)

    //============================================================
    // GPIO control, bits:
    // 0 - Green LED
    // 1 - Blue LED
    // 2 - Red LED  (write only: hardware limitation)
    // 3 - Pano button
    //============================================================

    val u_led_ctrl = Apb3Gpio(3, withReadSync = true)
    u_led_ctrl.io.gpio.write(0)             <> io.led_red
    u_led_ctrl.io.gpio.write(1)             <> io.led_green
    u_led_ctrl.io.gpio.write(2)             <> io.led_blue
    u_led_ctrl.io.gpio.read(0)              := io.led_red
    u_led_ctrl.io.gpio.read(1)              := io.led_green
    u_led_ctrl.io.gpio.read(2)              := io.led_blue

    apbMapping += u_led_ctrl.io.apb -> (0x10000, 4 kB)

    //============================================================
    // Various control signals
    //============================================================
    
    val has_jtag_uart     = (if (hasJtagUart) True else False)

    val u_ctrl = CCMiscCtrlStatus(nrCtrls = 0, nrStatus = 1)
    u_ctrl.io.status(0)           <> has_jtag_uart

    apbMapping += u_ctrl.io.apb -> (0x11000, 4 kB)

    //============================================================
    // JTAG Uart
    //============================================================

    val jtag_uart = if (hasJtagUart){
        val u_jtag_uart = new JtagUart()

        apbMapping += u_jtag_uart.io.apb -> (0x13000, 4 kB)
    }

    //============================================================
    // Uart
    //============================================================
    
    val uart = if (hasUart){

        val uartConfig = UartCtrlMemoryMappedConfig(
            uartCtrlConfig  = UartCtrlGenerics(),
            initConfig      = UartCtrlInitConfig(
              baudrate    = 9600
            ),
            txFifoDepth     = 255
        )

        val u_uart = Apb3UartCtrl(config = uartConfig)
        u_uart.io.uart      <> io.uart

        apbMapping += u_uart.io.apb -> (0x15000, 4 kB)
    }

    //============================================================
    // External APBs
    //============================================================
    
    // timer                                    0x00000
    // led_ctrl                                 0x10000
    // ctrl                                     0x11000
    // jtag_uart                                0x13000
    // uart                                     0x15000
    // usb_device                               0x20000
    
    apbMapping += io.usb_dev_apb       -> (0x20000, 8 kB)

    //============================================================
    // Local APB decoder
    //============================================================
    val apbDecoder = Apb3Decoder(
      master = u_cpu.io.apb,
      slaves = apbMapping
    )

}



# References

* [ULPI Specification 1.1](https://www.sparkfun.com/datasheets/Components/SMD/ULPI_v1_1.pdf)
* [Microchip/SMSC AN 19.17 - ULPI Design Guide](http://ww1.microchip.com/downloads/en/AppNotes/en562704.pdf)
* [Making USB Accessible @ Teardown 2019](https://greatscottgadgets.com/slides/making-usb-accessible-teardown-2019.pdf)

## USB code for FPGAs

* [ULPI Port](http://vr5.narod.ru/fpga/usb/index.html)

    * UTMI/ULPI bridge in VHDL
    * GPL2
    * Limited in functionalit:
        * Doesn't handle register write abort
        * No register read
        * No support for suspend

* [usb-serial - USB data transfer in VHDL](http://jorisvr.nl/article/usb-serial)

    * USB device controller in VHDL
    * Only supports serial class
    * UTMI interface
    * GPL2
    * Relatively well documented.

    * [VNA project](https://github.com/xaxaxa-dev/vna/blob/master/vhdl/ulpi_serial.vhd) uses
       [usb-serial](https://github.com/xaxaxa-dev/vna/tree/master/vhdl/third_party/fpga-usb-serial-20131205)
       and [ulpi_port](https://github.com/xaxaxa-dev/vna/blob/master/vhdl/third_party/ulpi_port.vhdl).

* [USBCore](https://github.com/ObKo/USBCore)

    Device controller. Most complete example out there?

    Has 2 main branches: 
    * the default one is 'old', which is a VHDL controller. Not updated in 6 years.
    * the 'master' branch has last been updated 6 months ago. Written in Verilog.

* [ULPI PMOD](https://github.com/ObKo/ULPI-Pmod)

    * USB3300 ULPI PHY
    * Uses 2 PMOD connectors, as expected.
    * 2 layer PCB. Gerbers are included.
    * Old version of KiCAD or incomplete library. Symbols in Schematc are not resolved... PCB loads fine.

* [lambdaUSB](https://github.com/lambdaconcept/lambdaUSB)

    USB 2.0 device controller written in nMigen.

    * "experimental stage and therefore incomplete."
    * Has a GUI to create a configuration.

* [ULPI in OpenVizsla](https://github.com/openvizsla/ov_ftdi/blob/master/software/fpga/ov3/ovhw/ulpi.py)


* [Digilent Genesys2 - USB Device Demo](https://github.com/Digilent/Genesys2/tree/master/Projects/USB_Device_Demo)

    This board has a TUSB1210.

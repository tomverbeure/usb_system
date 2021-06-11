
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

* [usb1_device](https://github.com/www-asics-ws/usb1_device)

    * USB 1.1 Device IP Core by [ASICSs World Services](www.asics.ws)
    * No CPU required
    * relies on `usb_phy` block (~poor man's UTMI block) whichi can be
      found on [opencores.org](https://opencores.org/projects/usb_phy).
    * "modified BSD for Resume style license"

* [usb2_dev](https://github.com/www-asics-ws/usb2_dev)

    * USB 2.0 Device IP Core by [ASICSs World Services](www.asics.ws)
    * UTMI interface
    * [Core documentation](https://github.com/www-asics-ws/usb2_dev/blob/master/doc/usb_doc.pdf) is 
       a very good USB introduction too.

* [Nitro USB Core](https://github.com/no2fpga/no2usb/tree/master)
    
    * USB 1.1 device using FPGA IOs
    * HW: CERN Open Hardware License v2
    * FW: LGPL 3.0

* [ULPI in OpenVizsla](https://github.com/openvizsla/ov_ftdi/blob/master/software/fpga/ov3/ovhw/ulpi.py)


* [Digilent Genesys2 - USB Device Demo](https://github.com/Digilent/Genesys2/tree/master/Projects/USB_Device_Demo)

    This board has a TUSB1210.

* [Luna](https://github.com/greatscottgadgets/luna/tree/main/luna/gateware/usb)

    Migen-based USB2 and USB3 core.


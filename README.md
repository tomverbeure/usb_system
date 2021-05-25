
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

* [ULPI in OpenVizsla](https://github.com/openvizsla/ov_ftdi/blob/master/software/fpga/ov3/ovhw/ulpi.py)

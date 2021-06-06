#! /usr/bin/env python3

from collections import namedtuple
from enum import Enum

import usb_crc
from pid import *
from usb_constants import *

# Types of wire events:
#
# Regular packets:
#
# data packet:
# - sync 
# - pid
# - data 
# - crc16
# - eop
#
# token packet:
# - sync
# - pid
# - address
# - endpoint
# - crc5
# - eop
# 
# handshake packet:
# - sync
# - pid
# - eop
#
# sof packet:
# - sync
# - pid
# - frame_nr 
# - crc5
# - eop
#
# Different than packets:
#
# - Drive constant value on the line
# - FS J,K,SE1,SE0
# - LS J,K,SE1,SE0
# - HS 

# Copied from ValentyUSB


# rx_started: 1 when sync word has been started.
# rx_active:  1 as soon as the sync word has been detected.
# rx_valid:   1 when a full byte has been detected and is ready to be sent to the link
# rx_error:   1 when an error has been detected
# rx_data:    value associated with rx_valid

RxState = namedtuple("RxState", [ 'rx_started', 'rx_active', 'rx_valid', 'rx_error', 'rx_data' ] )

class UsbWireEvent:

    def __init__(self):
        pass

class SetWire(UsbWireEvent):

    def __init__(self, ls):
        self.ls         = ls

    def encode_as_rx_line_state(self, speed):

        rx = []
        ls = self.ls
    
        rx.append(RxState(
                    rx_started = 0,
                    rx_active  = 0,
                    rx_valid   = 0,
                    rx_error   = 0,
                    rx_data    = 0))

        return (rx, ls)


class Packet(UsbWireEvent):

    def __init__(self, pid):

        super().__init__()
        self.pid        = pid

    def pid_as_byte(self):

        return self.pid | ((self.pid ^ 0xf) << 4)

    def encode_as_rx_line_state(self, speed):

        ls      =  ""

        # List of (RxActive, RxValid, RxData) tuples
        rx      = []

        if speed == UsbSpeed.FS or speed == UsbSpeed.LS:
            # For FS, each symbol is 5 clock cycles (60MHz/12)
            # For LS, each symbol is 40 clock cycles...

            sync =  "kjkjkjkk" 
            for x in range(len(sync)):
                rx.append(RxState(
                            rx_started = 1,
                            rx_active  = 0,
                            rx_valid   = 0,
                            rx_error   = 0,
                            rx_data    = 0))
            ls += sync

            last_byte = 0
            for b in self.raw_bytes():
                ls += "jjkjjkjk"        # line state doesn't matter after SYNC. Just some random value...
                for x in range(7):
                    rx.append(RxState(
                                rx_started = 1,
                                rx_active  = 1,
                                rx_valid   = 0,
                                rx_error   = 0,
                                rx_data    = last_byte))

                rx.append(RxState(
                            rx_started = 1,
                            rx_active  = 1,
                            rx_valid   = 1,
                            rx_error   = 0,
                            rx_data    = b))

                last_bytes = b

            ls += "__j"
            rx.append(RxState(
                        rx_started = 1,
                        rx_active  = 1,
                        rx_valid   = 0,
                        rx_error   = 0,
                        rx_data    = last_byte))
            rx.append(RxState(
                        rx_started = 1,
                        rx_active  = 1,
                        rx_valid   = 0,
                        rx_error   = 0,
                        rx_data    = last_byte))

            # FIXME:
            # Immediate after EOP or half into J ?
            # Right now, it's immediate after EOP...
            rx.append(RxState(
                        rx_started = 0,
                        rx_active  = 0,
                        rx_valid   = 0,
                        rx_error   = 0,
                        rx_data    = last_byte))

        elif speed == UsbSpeed.HS:
            # For HS, each symbol is 1/8th clock cycle
            # When there's no bit stuffing, there is 1 rx data per
            # clock.

            #FIXME:
            # Sync pattern is 32 symbols when there's no hub. Each hub
            # can eat 4 symbols, so after 5 hubs, the sync work can be 
            # as short as 12 symbols.
            # For now, always use 32 symbols
            sync = "jjjj"
            ls += sync

            rx.append(RxState(
                        rx_started = 1,
                        rx_active  = 0,
                        rx_valid   = 0,
                        rx_error   = 0,
                        rx_data    = 0))
            rx.append(RxState(
                        rx_started = 1,
                        rx_active  = 0,
                        rx_valid   = 0,
                        rx_error   = 0,
                        rx_data    = 0))
            rx.append(RxState(
                        rx_started = 1,
                        rx_active  = 0,
                        rx_valid   = 0,
                        rx_error   = 0,
                        rx_data    = 0))
            rx.append(RxState(
                        rx_started = 1,
                        rx_active  = 1,
                        rx_valid   = 0,
                        rx_error   = 0,
                        rx_data    = 0))

            # Right now, we're not doing any real bitstuffing, so
            # fake it by inserting a dummy cycle every once in a while.
            for nr, b in enumerate(self.raw_bytes()):
                ls += "j"
                rx.append(RxState(
                            rx_started = 1,
                            rx_active  = 1,
                            rx_valid   = 1,
                            rx_error   = 0,
                            rx_data    = b))
                if nr % 6 == 3:
                    ls += "j"
                    rx.append(RxState(
                                rx_started = 1,
                                rx_active  = 1,
                                rx_valid   = 0,
                                rx_error   = 0,
                                rx_data    = b))

            ls += "_"
            rx.append(RxState(
                        rx_started = 0,
                        rx_active  = 0,
                        rx_valid   = 0,
                        rx_error   = 0,
                        rx_data    = 0))

        return (rx, ls)

class DataPacket(Packet):
    
    def __init__(self, pid, data, crc16 = None, end_with_error = False):

        super().__init__(pid)

        self.data               = data
        self.end_with_error     = end_with_error

        if crc16 is None:
            self.crc16   = usb_crc.crc16(self.data)
        else: 
            self.crc16   = [ crc16 & 0xff, crc16 >> 8 ]

    def raw_bytes(self):

        db = []
        db.append(self.pid_as_byte())
        db += self.data
        db += self.crc16

        return db

class TokenPacket(Packet):

    def __init__(self, pid, address, endpoint, crc5 = None, end_with_error = False):

        super().__init__(pid)

        self.pid                = pid
        self.address            = address
        self.endpoint           = endpoint
        self.end_with_error     = end_with_error

        if crc5 is None:
            self.crc5   = usb_crc.crc5_token(address, endpoint)
        else: 
            self.crc5   = crc5

    def raw_bytes(self):

        db = []
        db.append(self.pid_as_byte())
        db.append(self.address | ((self.endpoint & 1)<<7))
        db.append((self.endpoint >> 1) | (self.crc5 << 3))

        return db

class HandshakePacket(Packet):

    def __init__(self, pid, end_with_error = False):

        super().__init__(pid)

        self.end_with_error     = end_with_error

    def raw_bytes(self):

        return [ self.pid_as_byte() ]

class SofPacket(Packet):

    def __init__(self, frame_nr, crc5 = None):

        super().__init__(PID.SOF)

        self.frame_nr   = frame_nr

        if crc5 is None:
            self.crc5   = usb_crc.crc5_sof(frame_nr)
        else:
            self.crc5   = crc5

    def raw_bytes(self):

        db = []

        db.append(self.pid_as_byte())
        db.append(self.frame_nr & 0xff)
        db.append((self.frame_nr >> 8) | (self.crc5 << 3))

        return db

if __name__ == "__main__":

    dp = DataPacket(PID.DATA0, [0,1,2,3,4,5,6])

    print(dp.raw_bytes())
    print(dp.encode_as_rx_line_state(UsbSpeed.FS)[1])
    print(dp.encode_as_rx_line_state(UsbSpeed.LS)[1])
    print(dp.encode_as_rx_line_state(UsbSpeed.HS)[1])

    tp = TokenPacket(PID.SETUP, 2, 3)
    print(tp.raw_bytes())
    print(tp.encode_as_rx_line_state(UsbSpeed.FS)[1])

    hp = HandshakePacket(PID.ACK)
    print(hp.raw_bytes())
    print(hp.encode_as_rx_line_state(UsbSpeed.FS)[1])

    sp = SofPacket(102)
    print(sp.raw_bytes())
    print(sp.encode_as_rx_line_state(UsbSpeed.FS)[1])

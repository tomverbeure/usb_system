
from enum import IntEnum

class PID(IntEnum):

    # Token pids
    SETUP   = 0b1101 # D
    OUT     = 0b0001 # 1
    IN      = 0b1001 # 9
    SOF     = 0b0101 # 5

    # Data pid
    DATA0   = 0b0011 # 3
    DATA1   = 0b1011 # B
    # USB HS only
    DATA2   = 0b0111 # B
    MDATA   = 0b1111 # F

    # Handshake pids
    ACK     = 0b0010 # 2
    NAK     = 0b1010 # A
    STALL   = 0b1110 # E
    # USB HS only
    NYET    = 0b0110 # 6

    # USB HS only
    PRE      = 0b1100 # C
    ERR      = 0b1100 # C
    SPLIT    = 0b1000 # 8
    PING     = 0b0100 # 4
    RESERVED = 0b0000 # 0



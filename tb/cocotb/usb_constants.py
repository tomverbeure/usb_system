
from enum import IntEnum, Enum

class UsbSpeed(Enum):
    LS      = 0
    FS      = 1
    HS      = 2


class LineState(Enum):
    SE0     = 0
    J       = 1
    K       = 2
    SE1     = 3


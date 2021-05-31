# Makefile

TOPLEVEL_LANG = verilog
VERILOG_SOURCES = Top_wrapper.v $(shell pwd)/../../spinal/Top.v ../top/jtag_uart.v 
TOPLEVEL = Top_wrapper
MODULE = test_ulpi

#SIM = verilator
#EXTRA_ARGS += --trace --trace-structs
#EXTRA_ARGS += -Wno-WIDTH

include $(shell cocotb-config --makefiles)/Makefile.sim


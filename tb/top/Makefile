

VERILOG_FILES 		= tb.v jtag_uart.v ulpi_phy.v ../../spinal/Top.sim.v 
SW_FILES			= $(wildcard ../../spinal/Top.*.bin)
SW_FILES_DST 		= $(notdir $(SW_FILES))


all: ./tb sw_files
	./tb

waves:
	gtkwave waves.vcd

./tb: $(VERILOG_FILES) 
	iverilog -D SIMULATION=1 -o $@ $(VERILOG_FILES)

sw_files: $(SW_FILES)
	cp $(SW_FILES) .

clean: 
	\rm -fr *.vcd ./tb *.bin

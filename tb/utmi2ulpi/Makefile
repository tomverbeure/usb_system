

VERILOG_FILES 		= tb.v ../../spinal/Utmi2Ulpi.v 


all: ./tb
	./tb

waves:
	gtkwave waves.vcd

./tb: $(VERILOG_FILES) 
	iverilog -D SIMULATION=1 -o $@ $(VERILOG_FILES)

clean: 
	\rm -fr *.vcd ./tb *.bin

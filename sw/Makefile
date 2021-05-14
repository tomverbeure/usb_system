
MEM_WORDS		= 4096
MEM_BYTES 		= 16384

MARCH 			= rv32imc
CPU_FREQ_MHZ	= 50
CC_OPT 			= -Os

OBJ_FILES       = start.o main.o lib.o jtag_uart.o

#TOOLS_PREFIX    = /opt/riscv32im/bin/
TOOLS_PREFIX 	= /opt/riscv64-unknown-elf-gcc-10.1.0-2020.08.2-x86_64-linux-ubuntu14/bin

#TARGET          = $(TOOLS_PREFIX)/riscv32-unknown-elf
TARGET          = $(TOOLS_PREFIX)/riscv64-unknown-elf

AS              = $(TARGET)-as
ASFLAGS         = -march=$(MARCH) -mabi=ilp32
LD              = $(TARGET)-gcc
LDFLAGS         = -march=$(MARCH) -g -ggdb -mabi=ilp32 -Wl,-Tsections.lds,-Map,progmem.map -ffreestanding -nostartfiles -Wl,--no-relax -Wl,--start-group 
CC              = $(TARGET)-gcc
CFLAGS          = -march=$(MARCH) -g -ggdb -mno-div -mabi=ilp32 -ffunction-sections -fdata-sections -Wall -Wextra -pedantic -DCPU_FREQ=$(CPU_FREQ_MHZ)000000 $(CC_OPT)
OBJCOPY         = $(TARGET)-objcopy
OBJDUMP         = $(TARGET)-objdump
READELF         = $(TARGET)-readelf

GDBGUI 			= gdbgui

.PHONY: all clean syntax time stat flash

#all: progmem.dis progmem.bin progmem0.coe progmem0.mif progmem0.hex progmem.mem
all: progmem.dis progmem.bin progmem4k.bin progmem.mem progmem.hex progmem0.mif

progmem.dis: progmem_dis.elf
	$(OBJDUMP) -s -D $< > $@

progmem.hex: progmem4k.bin
	$(OBJCOPY) --change-addresses 0x80000000 -O ihex -I binary $< $@

progmem0.hex: progmem.bin
	../misc/create_mif.rb -f hex -d $(MEM_WORDS) -w 8 -o 0 -i 4 $< > progmem0.hex
	../misc/create_mif.rb -f hex -d $(MEM_WORDS) -w 8 -o 1 -i 4 $< > progmem1.hex
	../misc/create_mif.rb -f hex -d $(MEM_WORDS) -w 8 -o 2 -i 4 $< > progmem2.hex
	../misc/create_mif.rb -f hex -d $(MEM_WORDS) -w 8 -o 3 -i 4 $< > progmem3.hex

progmem0.coe: progmem.bin
	../misc/create_mif.rb -f coe -d $(MEM_WORDS) -w 8 -o 0 -i 4 $< > progmem0.coe
	../misc/create_mif.rb -f coe -d $(MEM_WORDS) -w 8 -o 1 -i 4 $< > progmem1.coe
	../misc/create_mif.rb -f coe -d $(MEM_WORDS) -w 8 -o 2 -i 4 $< > progmem2.coe
	../misc/create_mif.rb -f coe -d $(MEM_WORDS) -w 8 -o 3 -i 4 $< > progmem3.coe

progmem0.mif: progmem.bin
	../misc/create_mif.rb -f mif -d $(MEM_WORDS) -w 8 -o 0 -i 4 $< > progmem0.mif
	../misc/create_mif.rb -f mif -d $(MEM_WORDS) -w 8 -o 1 -i 4 $< > progmem1.mif
	../misc/create_mif.rb -f mif -d $(MEM_WORDS) -w 8 -o 2 -i 4 $< > progmem2.mif
	../misc/create_mif.rb -f mif -d $(MEM_WORDS) -w 8 -o 3 -i 4 $< > progmem3.mif

progmem.mif: progmem.bin
	../misc/create_mif.rb -f mif -d $(MEM_WORDS) -w 32 $< > progmem.mif

progmem.mem: progmem.bin
	../misc/create_mif.rb -f mem -d $(MEM_WORDS) -w 32 $< > progmem.mem

progmem.bin: progmem.elf
	$(OBJCOPY) -O binary $< $@

progmem4k.bin: progmem.elf
	$(OBJCOPY) --pad-to=$(MEM_BYTES) -O binary $< $@

progmem.elf: $(OBJ_FILES) top_defines.h sections.lds Makefile 
	$(LD) $(LDFLAGS) -o $@ $(OBJ_FILES) -lm 

progmem_dis.elf: $(OBJ_FILES) top_defines.h sections.lds Makefile
	$(LD) $(LDFLAGS) -o $@ $(OBJ_FILES) -lm 

main.o: top_defines.h

main.c: top_defines.h

ocd:
	/opt/openocd-vex/bin/openocd -f /opt/openocd-vex/share/openocd/scripts/interface/ftdi/digilent_jtag_smt2.cfg \
		-c "adapter speed 1000; transport select jtag" \
		-f "vexriscv_init.cfg"
		#-c "jtag newtap fpga cpu -irlen 4 -expected-id 0x10001fff"

gdb:
	$(TARGET)-gdb progmem.elf \
		-ex "target remote localhost:3333"
gdbgui:
	$(GDBGUI) -g '$(TARGET)-gdb progmem.elf -ex "target remote localhost:3333"'

jterm:
	LD_LIBRARY_PATH=$(QUARTUS_ROOTDIR)/linux64 nios2-terminal

clean:
	\rm -fr *.o *.hex *.elf *.dis *.bin *.coe *.map *.mif *.mem *.funcs *.globs

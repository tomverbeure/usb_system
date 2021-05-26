## Generated SDC file "/home/tom/projects/usb_system/usb_system.sdc"

## Copyright (C) 2020  Intel Corporation. All rights reserved.
## Your use of Intel Corporation's design tools, logic functions 
## and other software and tools, and any partner logic 
## functions, and any output files from any of the foregoing 
## (including device programming or simulation files), and any 
## associated documentation or information are expressly subject 
## to the terms and conditions of the Intel Program License 
## Subscription Agreement, the Intel Quartus Prime License Agreement,
## the Intel FPGA IP License Agreement, or other applicable license
## agreement, including, without limitation, that your use is for
## the sole purpose of programming logic devices manufactured by
## Intel and sold by Intel or its authorized distributors.  Please
## refer to the applicable agreement for further details, at
## https://fpgasoftware.intel.com/eula.


## VENDOR  "Altera"
## PROGRAM "Quartus Prime"
## VERSION "Version 20.1.1 Build 720 11/11/2020 SJ Lite Edition"

## DATE    "Fri May 21 20:36:13 2021"

##
## DEVICE  "10M50DAF484C6GES"
##


#**************************************************************
# Time Information
#**************************************************************

set_time_format -unit ns -decimal_places 3



#**************************************************************
# Create Clock
#**************************************************************

create_clock -name {altera_reserved_tck} -period 100.000 -waveform { 0.000 50.000 } [get_ports {altera_reserved_tck}]
create_clock -name {osc_clk_in} -period 20.000 -waveform { 0.000 10.000 } [get_ports {osc_clk_in}]
create_clock -name {ulpi_clk} -period 16.600 -waveform { 0.000 8.333 } [get_ports {ulpi_clk}]
#create_clock -name cpu_clk -period 20.000 [get_pins {u_pll|altpll_component|auto_generated|pll1|clk[0]}]
#create_clock -name tap_clk -period 5.000 [get_pins {u_pll|altpll_component|auto_generated|pll1|clk[1]}]

create_generated_clock -name cpu_clk -source {u_pll|altpll_component|auto_generated|pll1|inclk[0]} -divide_by 1 -multiply_by 1 -duty_cycle 50.00 { u_pll|altpll_component|auto_generated|pll1|clk[0] }
create_generated_clock -name tap_clk -source {u_pll|altpll_component|auto_generated|pll1|inclk[0]} -divide_by 1 -multiply_by 4 -duty_cycle 50.00 { u_pll|altpll_component|auto_generated|pll1|clk[1] }
create_generated_clock -name ulpi_clk_internal -source {u_ulpi_pll|altpll_component|auto_generated|pll1|inclk[0]} -divide_by 1 -multiply_by 1 -duty_cycle 50.00 -phase 0 { u_ulpi_pll|altpll_component|auto_generated|pll1|clk[0] }

#derive_pll_clocks
derive_clock_uncertainty


#**************************************************************
# Create Generated Clock
#**************************************************************



#**************************************************************
# Set Clock Latency
#**************************************************************



#**************************************************************
# Set Clock Uncertainty
#**************************************************************



#**************************************************************
# Set Input Delay
#**************************************************************

set_input_delay -add_delay  -clock [get_clocks {ulpi_clk}]  9.000 [get_ports {ulpi_data[*]}] -max
set_input_delay -add_delay  -clock [get_clocks {ulpi_clk}]  9.000 [get_ports {ulpi_direction}] -max
set_input_delay -add_delay  -clock [get_clocks {ulpi_clk}]  9.000 [get_ports {ulpi_nxt}] -max

set_input_delay -add_delay  -clock [get_clocks {ulpi_clk}]  -0.000 [get_ports {ulpi_data[*]}] -min
set_input_delay -add_delay  -clock [get_clocks {ulpi_clk}]  -0.000 [get_ports {ulpi_direction}] -min
set_input_delay -add_delay  -clock [get_clocks {ulpi_clk}]  -0.000 [get_ports {ulpi_nxt}] -min


#**************************************************************
# Set Output Delay
#**************************************************************

set_output_delay -add_delay  -clock [get_clocks {ulpi_clk}]  6.000 [get_ports {ulpi_data[*]}]
set_output_delay -add_delay  -clock [get_clocks {ulpi_clk}]  6.000 [get_ports {ulpi_stp}]

#**************************************************************
# Set Clock Groups
#**************************************************************

set_clock_groups -asynchronous -group [get_clocks {altera_reserved_tck}] 


#**************************************************************
# Set False Path
#**************************************************************

set_false_path -from [get_registers {*|alt_jtag_atlantic:*|jupdate}] -to [get_registers {*|alt_jtag_atlantic:*|jupdate1*}]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|rdata[*]}] -to [get_registers {*|alt_jtag_atlantic*|td_shift[*]}]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|read}] -to [get_registers {*|alt_jtag_atlantic:*|read1*}]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|read_req}] 
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|rvalid}] -to [get_registers {*|alt_jtag_atlantic*|td_shift[*]}]
set_false_path -from [get_registers {*|t_dav}] -to [get_registers {*|alt_jtag_atlantic:*|tck_t_dav}]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|user_saw_rvalid}] -to [get_registers {*|alt_jtag_atlantic:*|rvalid0*}]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|wdata[*]}] -to [get_registers *]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|write}] -to [get_registers {*|alt_jtag_atlantic:*|write1*}]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|write_stalled}] -to [get_registers {*|alt_jtag_atlantic:*|t_ena*}]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|write_stalled}] -to [get_registers {*|alt_jtag_atlantic:*|t_pause*}]
set_false_path -from [get_registers {*|alt_jtag_atlantic:*|write_valid}] 
set_false_path -to [get_pins -nocase -compatibility_mode {*|alt_rst_sync_uq1|altera_reset_synchronizer_int_chain*|clrn}]

set_false_path -from [get_clocks {osc_clk_in}] -to [get_clocks {ulpi_clk}]
set_false_path -from [get_clocks {ulpi_clk}] -to [get_clocks {osc_clk_in}]
set_false_path -from [get_clocks {ulpi_clk}] -to [get_clocks {cpu_clk}]
set_false_path -from [get_clocks {ulpi_clk}] -to [get_clocks {tap_clk}]

set_false_path -from [get_clocks {cpu_clk}] -to [get_clocks {ulpi_clk}]
set_false_path -from [get_clocks {cpu_clk}] -to [get_clocks {ulpi_clk_internal}]


set_false_path -from [get_clocks {ulpi_clk_internal}] -to [get_clocks {cpu_clk}]
set_false_path -from [get_clocks {ulpi_clk_internal}] -to [get_clocks {tap_clk}]

set_false_path  -to {sld_signaltap:*}

#**************************************************************
# Set Multicycle Path
#**************************************************************

#set_multicycle_path -from {ulpi_direction} -to {ulpi_data[*]} -setup -end 2
#set_multicycle_path -from {ulpi_direction} -to {ulpi_data[*]} -hold  -end 1


#**************************************************************
# Set Maximum Delay
#**************************************************************



#**************************************************************
# Set Minimum Delay
#**************************************************************



#**************************************************************
# Set Input Transition
#**************************************************************





#**************************************************************
# Time Information
#**************************************************************

set_time_format -unit ns -decimal_places 3

#**************************************************************
# Create Clock
#**************************************************************

create_clock -name {altera_reserved_tck} -period 100.000  [get_ports {altera_reserved_tck}]
create_clock -name {osc_clk_in} -period 20.000 [get_ports {osc_clk_in}]
create_clock -name {jtag_clk}   -period 50.001 [get_ports {jtag_tck}]
create_clock -name {ulpi_clk}   -period 16.600 [get_ports {ulpi_clk}]

# ulpi_clk_phy is a virtual clock at the pins of the PHY. There is a 2.4ns delay from the PHY clock pin
# to the link clock pin. 
create_clock -name {ulpi_clk_phy} -period 16.600 
set_clock_latency -source 2.4 [get_clocks {ulpi_clk}]

create_generated_clock -name cpu_clk -source {pll_u_pll|altpll_component|auto_generated|pll1|inclk[0]} -divide_by 1 -multiply_by 1 { pll_u_pll|altpll_component|auto_generated|pll1|clk[0] }

# PLL phase aligns input to output, but with a -52.5 degree delay (-2.4/16.6*360 ~ -52.5)
create_generated_clock -name ulpi_clk_int -source {ulpi_pll_u_ulpi_pll|altpll_component|auto_generated|pll1|inclk[0]} -divide_by 1 -multiply_by 1 -phase -52.5 { ulpi_pll_u_ulpi_pll|altpll_component|auto_generated|pll1|clk[0] }

derive_pll_clocks
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

set_input_delay -add_delay  -clock [get_clocks {ulpi_clk_phy}]  9.0 [get_ports {ulpi_data[*]}] -max
set_input_delay -add_delay  -clock [get_clocks {ulpi_clk_phy}]  9.0 [get_ports {ulpi_direction}] -max
set_input_delay -add_delay  -clock [get_clocks {ulpi_clk_phy}]  9.0 [get_ports {ulpi_nxt}] -max

set_input_delay -add_delay  -clock [get_clocks {ulpi_clk_phy}]  0.0 [get_ports {ulpi_data[*]}] -min
set_input_delay -add_delay  -clock [get_clocks {ulpi_clk_phy}]  0.0 [get_ports {ulpi_direction}] -min
set_input_delay -add_delay  -clock [get_clocks {ulpi_clk_phy}]  0.0 [get_ports {ulpi_nxt}] -min

set_input_delay -add_delay -clock [get_clocks jtag_clk] 2 [get_ports jtag_tdi] -max
set_input_delay -add_delay -clock [get_clocks jtag_clk] 2 [get_ports jtag_tms] -max

set_input_delay -add_delay -clock [get_clocks jtag_clk] 0 [get_ports jtag_tdi] -min
set_input_delay -add_delay -clock [get_clocks jtag_clk] 0 [get_ports jtag_tms] -min

#**************************************************************
# Set Output Delay
#**************************************************************

# See https://electronics.stackexchange.com/questions/570538/different-output-delays-for-internal-to-output-and-input-to-output-path

# 16.6 - 6 
# get_max_delay includes the delay from clock tree.
set_max_delay -from [get_registers *] -to [get_ports {ulpi_data[*]}] 10.6
set_max_delay -from [get_registers *] -to [get_ports {ulpi_stp}]     10.6

set_min_delay -from [get_registers *] -to [get_ports {ulpi_data[*]}] 0
set_min_delay -from [get_registers *] -to [get_ports {ulpi_stp}]     0

# - Not: 16.6 - 9.0, because the 9.0 gets added with the earlier set_input_delay
set_max_delay -from [get_ports {ulpi_direction}] -to [get_ports {ulpi_data[*]}] 16.6
set_min_delay -from [get_ports {ulpi_direction}] -to [get_ports {ulpi_data[*]}] 0

#**************************************************************
# Set Clock Groups
#**************************************************************

set_clock_groups -asynchronous \
	-group {altera_reserved_tck} \
	-group {osc_clk_in} \
	-group {ulpi_clk, ulpi_clk_int} \
	-group {cpu_clk}

#**************************************************************
# Set False Path
#**************************************************************

# JTAG UART
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

# FlowCCByToggle synchronization primitive, used in JtagBridge
set_false_path -from [get_registers *|FlowCCByToggle*|inputArea_target] -to [get_registers *|FlowCCByToggle*|*buffers_0]
set_false_path -from [get_registers *|FlowCCByToggle*|inputArea_data_fragment*] -to [get_registers *|FlowCCByToggle*|outputArea_flow_regNext_payload_fragment*]
set_false_path -from [get_registers *|FlowCCByToggle*|inputArea_data_last] -to [get_registers *|FlowCCByToggle*|outputArea_flow_regNext_payload_last]

set_false_path -from [get_registers *|JtagBridge:*|system_rsp_valid]
set_false_path -from [get_registers *|JtagBridge:*|system_rsp_payload_data*]
set_false_path -from [get_registers *|JtagBridge:*|system_rsp_payload_error]

set_false_path -from [get_ports ulpi_fault_]

#set_false_path  -to {sld_signaltap:*}

#**************************************************************
# Set Multicycle Path
#**************************************************************


#**************************************************************
# Set Maximum Delay
#**************************************************************



#**************************************************************
# Set Minimum Delay
#**************************************************************



#**************************************************************
# Set Input Transition
#**************************************************************




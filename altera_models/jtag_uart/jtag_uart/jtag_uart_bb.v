
module jtag_uart (
	clk_clk,
	reset_reset_n,
	avbus_chipselect,
	avbus_address,
	avbus_read_n,
	avbus_readdata,
	avbus_write_n,
	avbus_writedata,
	avbus_waitrequest);	

	input		clk_clk;
	input		reset_reset_n;
	input		avbus_chipselect;
	input		avbus_address;
	input		avbus_read_n;
	output	[31:0]	avbus_readdata;
	input		avbus_write_n;
	input	[31:0]	avbus_writedata;
	output		avbus_waitrequest;
endmodule

	jtag_uart u0 (
		.clk_clk           (<connected-to-clk_clk>),           //   clk.clk
		.reset_reset_n     (<connected-to-reset_reset_n>),     // reset.reset_n
		.avbus_chipselect  (<connected-to-avbus_chipselect>),  // avbus.chipselect
		.avbus_address     (<connected-to-avbus_address>),     //      .address
		.avbus_read_n      (<connected-to-avbus_read_n>),      //      .read_n
		.avbus_readdata    (<connected-to-avbus_readdata>),    //      .readdata
		.avbus_write_n     (<connected-to-avbus_write_n>),     //      .write_n
		.avbus_writedata   (<connected-to-avbus_writedata>),   //      .writedata
		.avbus_waitrequest (<connected-to-avbus_waitrequest>)  //      .waitrequest
	);


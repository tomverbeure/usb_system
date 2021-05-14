config jtag_uart_cfg;
		design jtag_uart;
		instance jtag_uart.jtag_uart_0 use jtag_uart_altera_avalon_jtag_uart_170.jtag_uart_altera_avalon_jtag_uart_170_wsv57wi;
		instance jtag_uart.rst_controller use jtag_uart_altera_reset_controller_170.altera_reset_controller;
endconfig


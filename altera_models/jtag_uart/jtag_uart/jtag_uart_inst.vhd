	component jtag_uart is
		port (
			clk_clk           : in  std_logic                     := 'X';             -- clk
			reset_reset_n     : in  std_logic                     := 'X';             -- reset_n
			avbus_chipselect  : in  std_logic                     := 'X';             -- chipselect
			avbus_address     : in  std_logic                     := 'X';             -- address
			avbus_read_n      : in  std_logic                     := 'X';             -- read_n
			avbus_readdata    : out std_logic_vector(31 downto 0);                    -- readdata
			avbus_write_n     : in  std_logic                     := 'X';             -- write_n
			avbus_writedata   : in  std_logic_vector(31 downto 0) := (others => 'X'); -- writedata
			avbus_waitrequest : out std_logic                                         -- waitrequest
		);
	end component jtag_uart;

	u0 : component jtag_uart
		port map (
			clk_clk           => CONNECTED_TO_clk_clk,           --   clk.clk
			reset_reset_n     => CONNECTED_TO_reset_reset_n,     -- reset.reset_n
			avbus_chipselect  => CONNECTED_TO_avbus_chipselect,  -- avbus.chipselect
			avbus_address     => CONNECTED_TO_avbus_address,     --      .address
			avbus_read_n      => CONNECTED_TO_avbus_read_n,      --      .read_n
			avbus_readdata    => CONNECTED_TO_avbus_readdata,    --      .readdata
			avbus_write_n     => CONNECTED_TO_avbus_write_n,     --      .write_n
			avbus_writedata   => CONNECTED_TO_avbus_writedata,   --      .writedata
			avbus_waitrequest => CONNECTED_TO_avbus_waitrequest  --      .waitrequest
		);


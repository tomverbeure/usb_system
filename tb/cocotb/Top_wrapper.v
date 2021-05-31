`default_nettype none
`timescale 1ns/100ps

module Top_wrapper(
    input               osc_clk_in,
  
    input               ulpi_clk,
    output              ulpi_reset_,
    input      [7:0]    ulpi_data2link,
    output     [7:0]    ulpi_data2phy,
    output              ulpi_data_ena,
    input               ulpi_direction,
    output              ulpi_stp,
    input               ulpi_nxt,
  
    output              led0,
    output              led1,
    output              led2,
    output              led3
  );

`ifdef COCOTB_SIM
initial begin
  $dumpfile ("Top.vcd");
  $dumpvars (0, Top_wrapper);
  #1;
end
`endif

    wire [7:0]      ulpi_data_writeEnable;

    assign ulpi_data_ena = ulpi_data_writeEnable[0];

    Top u_top(
        .osc_clk_in(osc_clk_in),
        .jtag_tck(1'b0),
        .jtag_tms(1'b0),
        .jtag_tdi(1'b0),
        .led0(led0),
        .led1(led1),
        .led2(led2),
        .led3(led3),

        .ulpi_clk(ulpi_clk),
        .ulpi_reset_(ulpi_reset_),
        .ulpi_data_read(ulpi_data2link),
        .ulpi_data_write(ulpi_data2phy),
        .ulpi_data_writeEnable(ulpi_data_writeEnable),
        .ulpi_direction(ulpi_direction),
        .ulpi_stp(ulpi_stp),
        .ulpi_nxt(ulpi_nxt)
    );
    

endmodule

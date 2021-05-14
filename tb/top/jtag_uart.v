module jtag_uart (
        av_chipselect,
        av_address,
        av_read_n,
        av_readdata,
        av_write_n,
        av_writedata,
        av_waitrequest,
        clk_clk,
        irq_irq,
        reset_reset_n);        

    input                av_chipselect;
    output reg           av_waitrequest;
    input                av_address;
    input                av_write_n;
    input      [31:0]    av_writedata;
    input                av_read_n;
    output reg [31:0]    av_readdata;
    input                clk_clk;
    output               irq_irq;
    input                reset_reset_n;

    reg cs_d;

    always @(posedge clk_clk) begin
        av_waitrequest      <= 1'b1;
        av_readdata         <= 32'h0001_0000;

        if (av_chipselect && !cs_d) begin
            av_waitrequest  <= 1'b0;
        end

        cs_d    <= av_chipselect;
    end

    assign irq_irq      = 1'b0;

endmodule

`default_nettype none
`timescale 1ns/100ps

module tb;

    reg clk = 0;
    reg ulpi_clk = 0;

    always begin
        clk=0;
        #50;
        clk=1;
        #50;
    end

    always begin
        ulpi_clk=0;
        #8.3;
        ulpi_clk=1;
        #8.3;
    end

    reg ulpi_reset;
    initial begin
        ulpi_reset = 1'b1;
        repeat(5) @(posedge ulpi_clk);
        ulpi_reset = 1'b0;
    end

    initial begin
        $dumpfile("waves.vcd");
        $dumpvars();
        repeat(2000) @(posedge clk);
        $finish;
    end

    wire led0, led1, led2, button;

    wire       [7:0]    ulpi_data_read;
    wire       [7:0]    ulpi_data_write;
    wire       [7:0]    ulpi_data_writeEnable;
    wire                ulpi_direction;
    wire                ulpi_stp;
    wire                ulpi_nxt;

    Top u_top(
        .osc_clk_in(clk),
        .jtag_tck(1'b0),
        .jtag_tms(1'b0),
        .jtag_tdi(1'b0),
        .led0(led0),
        .led1(led1),
        .led2(led2),

        .ulpi_clk(ulpi_clk),
        .ulpi_data_read(ulpi_data_read),
        .ulpi_data_write(ulpi_data_write),
        .ulpi_data_writeEnable(ulpi_data_writeEnable),
        .ulpi_direction(ulpi_direction),
        .ulpi_stp(ulpi_stp),
        .ulpi_nxt(ulpi_nxt)
    );

    reg led0_d, led1_d, led2_d;

    always @(posedge clk) begin
        if (led0 != led0_d) begin
            $display("%d: led0 changed to %d", $time, led0);
        end
        if (led1 != led1_d) begin
            $display("%d: led1 changed to %d", $time, led1);
        end
        if (led2 != led2_d) begin
            $display("%d: led2 changed to %d", $time, led2);
        end

        led0_d <= led0;
        led1_d <= led1;
        led2_d <= led2;
    end

    wire [7:0] ulpi_data;

    assign ulpi_data = ulpi_data_writeEnable[0] ? ulpi_data_write : 8'hz;
    assign ulpi_data_read = ulpi_data;

    ulpi_phy u_ulpi_phy(
        .ulpi_clk           (ulpi_clk),
        .ulpi_reset         (ulpi_reset),
        .ulpi_data          (ulpi_data),
        .ulpi_stp           (ulpi_stp),
        .ulpi_direction     (ulpi_direction),
        .ulpi_nxt           (ulpi_nxt)
    );

endmodule

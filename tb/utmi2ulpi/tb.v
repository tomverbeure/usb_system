`default_nettype none
`timescale 1ns/100ps

module tb;

    reg clk         = 0;
    reg reset       = 1;

    initial begin
        $dumpfile("waves.vcd");
        $dumpvars();
        repeat(2000) @(posedge clk);
        $finish;
    end

    always begin
        clk=0;
        #50;
        clk=1;
        #50;
    end

    initial begin
        reset = 1'b1;
        repeat(10) @(posedge clk);
        reset = 1'b0;
    end

    reg                 utmi_tx_valid;
    wire                utmi_tx_ready;
    reg        [7:0]    utmi_data_in;
    wire                utmi_rx_valid;
    wire       [7:0]    utmi_data_out;
    reg                 utmi_reset;
    reg                 utmi_suspend_m;
    reg        [1:0]    utmi_xcvr_select;
    reg                 utmi_term_select;
    reg        [1:0]    utmi_op_mode;
    wire                utmi_rx_active;
    wire                utmi_rx_error;
    wire       [1:0]    utmi_line_state;

    reg                 func_reset;

    reg                 reg_req;
    wire                reg_req_done;
    reg                 reg_rd;
    reg        [7:0]    reg_addr;
    reg        [7:0]    reg_wdata;
    wire       [7:0]    reg_rdata;

    initial begin
        utmi_reset          <= 1'b0;
        utmi_tx_valid       <= 1'b0;
        utmi_data_in        <= 8'd0;
        utmi_suspend_m      <= 1'b1;
        utmi_xcvr_select    <= 2'd1;
        utmi_term_select    <= 1'b1;
        utmi_op_mode        <= 2'd0;

        reg_req             <= 1'b0;

        repeat(100) @(posedge clk);

        utmi_term_select    <= 1'b0;
        repeat(100) @(posedge clk);

        utmi_term_select    <= 1'b1;
        repeat(100) @(posedge clk);

        reg_req             <= 1'b1;
        reg_rd              <= 1'b0;
        reg_addr            <= 8'h04;
        reg_wdata           <= 8'h5a;
        repeat(20) @(posedge clk);
        reg_req             <= 1'b0;
        repeat(20) @(posedge clk);
        reg_req             <= 1'b1;
        reg_rd              <= 1'b1;
        reg_addr            <= 8'h04;
        repeat(20) @(posedge clk);
        reg_req             <= 1'b0;
        repeat(20) @(posedge clk);

        $finish;
    end

    wire                ulpi_data_ena;
    wire       [7:0]    ulpi_data_out;
    reg        [7:0]    ulpi_data_in;
    reg                 ulpi_direction;
    wire                ulpi_stp;
    reg                 ulpi_nxt;


    Utmi2Ulpi u_utmi2ulpi (
            .clk(clk),
            .reset(reset),

            .io_ulpi_data_ena(ulpi_data_ena),
            .io_ulpi_data_out(ulpi_data_out),
            .io_ulpi_data_in(ulpi_data_in),
            .io_ulpi_direction(ulpi_direction),
            .io_ulpi_stp(ulpi_stp),
            .io_ulpi_nxt(ulpi_nxt),

            .io_utmi_tx_valid(utmi_tx_valid),
            .io_utmi_tx_ready(utmi_tx_ready),
            .io_utmi_data_in(utmi_data_in),
            .io_utmi_rx_valid(utmi_rx_valid),
            .io_utmi_data_out(utmi_data_out),
            .io_utmi_reset(utmi_reset),
            .io_utmi_suspend_m(utmi_suspend_m),
            .io_utmi_xcvr_select(utmi_xcvr_select),
            .io_utmi_term_select(utmi_term_select),
            .io_utmi_op_mode(utmi_op_mode),
            .io_utmi_rx_active(utmi_rx_active),
            .io_utmi_rx_error(utmi_rx_error),
            .io_utmi_line_state(utmi_line_state),

            .io_func_reset(func_reset),

            .io_reg_req(reg_req),
            .io_reg_req_done(reg_req_done),
            .io_reg_rd(reg_rd),
            .io_reg_addr(reg_addr),
            .io_reg_wdata(reg_wdata),
            .io_reg_rdata(reg_rdata)
        );

    localparam ULPI_IDLE                    = 0;
    localparam ULPI_REG_ADDR                = 1;
    localparam ULPI_REG_WDATA               = 2;
    localparam ULPI_REG_TURNAROUND          = 3;
    localparam ULPI_REG_RDATA               = 4;

    reg [4:0] cur_ulpi_state, nxt_ulpi_state;

    reg [7:0] ulpi_reg_addr,     ulpi_reg_addr_nxt;
    reg [7:0] ulpi_reg_wdata,    ulpi_reg_wdata_nxt;

    always  @(*) begin
        func_reset              = 1'b0;

        ulpi_direction          = 1'b0;
        ulpi_nxt                = 1'b0;
        ulpi_data_in            = 8'd0;

        nxt_ulpi_state          = cur_ulpi_state;

        ulpi_reg_addr_nxt       = ulpi_reg_addr;
        ulpi_reg_wdata_nxt      = ulpi_reg_wdata;

        case(cur_ulpi_state)
            ULPI_IDLE: begin
                ulpi_direction          = 1'b0;
                ulpi_nxt                = 1'b0;
                ulpi_data_in            = 8'd0;

                if (ulpi_data_out[7:6] == 2'b10 || ulpi_data_out[7:6] == 2'b11) begin
                    nxt_ulpi_state      = ULPI_REG_ADDR;
                end
            end

            ULPI_REG_ADDR: begin
                ulpi_nxt                = 1'b1;

                ulpi_reg_addr_nxt       = ulpi_data_out[5:0];

                if (ulpi_data_out[7:6] == 2'b10) begin
                    nxt_ulpi_state          = ULPI_REG_WDATA;
                end
                else begin
                    nxt_ulpi_state          = ULPI_REG_TURNAROUND;
                end
            end

            ULPI_REG_WDATA: begin
                ulpi_nxt                = 1'b1;
                ulpi_reg_wdata_nxt      = ulpi_data_out;
                nxt_ulpi_state          = ULPI_IDLE;
            end

            ULPI_REG_TURNAROUND: begin
                ulpi_nxt                = 1'b0;
                ulpi_direction          = 1'b1;

                nxt_ulpi_state          = ULPI_REG_RDATA;
            end

            ULPI_REG_RDATA: begin
                ulpi_nxt                = 1'b0;
                ulpi_direction          = 1'b1;
                ulpi_data_in            = ulpi_reg_wdata_nxt;

                nxt_ulpi_state          = ULPI_IDLE;
            end
        endcase

    end
    
    always @(posedge clk) begin
        if (reset) begin
            cur_ulpi_state      <= ULPI_IDLE;
        end
        else begin
            cur_ulpi_state      <= nxt_ulpi_state;
            ulpi_reg_addr       <= ulpi_reg_addr_nxt;
            ulpi_reg_wdata      <= ulpi_reg_wdata_nxt;
        end
    end

endmodule

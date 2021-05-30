
module ulpi_phy(
        input               ulpi_clk,
        input               ulpi_reset, 
        inout [7:0]         ulpi_data,
        input               ulpi_stp,
        output reg          ulpi_direction,
        output reg          ulpi_nxt
    );

    localparam ULPI_IDLE                    = 0;
    localparam ULPI_REG_ADDR                = 1;
    localparam ULPI_REG_WDATA               = 2;
    localparam ULPI_REG_TURNAROUND          = 3;
    localparam ULPI_REG_RDATA               = 4;

    reg [4:0] cur_ulpi_state, nxt_ulpi_state;

    reg [7:0] ulpi_reg_addr,     ulpi_reg_addr_nxt;
    reg [7:0] ulpi_reg_wdata,    ulpi_reg_wdata_nxt;

    reg  [7:0] ulpi_data_in;
    wire [7:0] ulpi_data_out;

    assign ulpi_data_out = ulpi_data;
    assign ulpi_data     = (ulpi_direction && ! turn_around) ? ulpi_data_in : 8'bz;

    always  @(*) begin
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

    reg ulpi_direction_d;
    
    always @(posedge ulpi_clk) begin
        if (ulpi_reset) begin
            cur_ulpi_state      <= ULPI_IDLE;
            ulpi_direction_d    <= 1'b0;
        end
        else begin
            cur_ulpi_state      <= nxt_ulpi_state;
            ulpi_reg_addr       <= ulpi_reg_addr_nxt;
            ulpi_reg_wdata      <= ulpi_reg_wdata_nxt;
        end
    end

    wire turn_around;
    assign turn_around = ulpi_direction != ulpi_direction_d;

endmodule

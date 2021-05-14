// Generator : SpinalHDL v1.4.3    git head : adf552d8f500e7419fff395b7049228e4bc5de26
// Component : Utmi2Ulpi


`define UlpiState_binary_sequential_type [3:0]
`define UlpiState_binary_sequential_Idle 4'b0000
`define UlpiState_binary_sequential_TxStart 4'b0001
`define UlpiState_binary_sequential_TxData 4'b0010
`define UlpiState_binary_sequential_RegWrWaitAddrAck 4'b0011
`define UlpiState_binary_sequential_RegWrWaitDataAck 4'b0100
`define UlpiState_binary_sequential_RegWrStp 4'b0101
`define UlpiState_binary_sequential_RegRdAddr 4'b0110
`define UlpiState_binary_sequential_RegRdTurn 4'b0111
`define UlpiState_binary_sequential_RegRdData 4'b1000


module Utmi2Ulpi (
  input               io_ulpi_clk,
  output              io_ulpi_data_ena,
  output     [7:0]    io_ulpi_data_out,
  input      [7:0]    io_ulpi_data_in,
  input               io_ulpi_direction,
  output              io_ulpi_stp,
  input               io_ulpi_nxt,
  output              io_ulpi_reset,
  output              io_utmi_clk,
  input               io_utmi_tx_valid,
  output              io_utmi_tx_ready,
  input      [7:0]    io_utmi_data_in,
  output              io_utmi_rx_valid,
  output     [7:0]    io_utmi_data_out,
  input               io_utmi_reset,
  input               io_utmi_suspend_m,
  input      [1:0]    io_utmi_xcvr_select,
  input               io_utmi_term_select,
  input      [1:0]    io_utmi_op_mode,
  output              io_utmi_rx_active,
  output              io_utmi_rx_error,
  output     [1:0]    io_utmi_line_state,
  input               io_func_reset
);
  wire                _zz_Utmi2Ulpi_1;
  wire                _zz_Utmi2Ulpi_2;
  wire       [1:0]    _zz_Utmi2Ulpi_3;
  wire       [1:0]    _zz_Utmi2Ulpi_4;
  reg                 ulpi_reset_ = 1'b0;
  reg        [1:0]    ulpi_domain_utmi_xcvr_select;
  reg                 ulpi_domain_utmi_term_select;
  reg        [1:0]    ulpi_domain_utmi_op_mode;
  reg                 ulpi_domain_utmi_suspend_m;
  reg                 ulpi_domain_func_reset;
  reg                 io_ulpi_direction_regNext;
  wire                ulpi_domain_turn_around;
  wire                ulpi_domain_func_ctrl_diff;
  reg                 ulpi_domain_func_ctrl_wr_pending;
  reg        [7:0]    ulpi_domain_ulpi_data_out;
  reg                 ulpi_domain_ulpi_stp;
  reg                 ulpi_domain_utmi_rx_active;
  reg                 ulpi_domain_utmi_rx_error;
  wire                ulpi_domain_utmi_rx_valid;
  wire       [7:0]    ulpi_domain_utmi_rx_data;
  reg                 ulpi_domain_utmi_tx_ready;
  reg        [1:0]    ulpi_domain_utmi_line_state;
  reg                 ulpi_domain_utmi_sess_end;
  reg                 ulpi_domain_utmi_sess_valid;
  reg                 ulpi_domain_utmi_vbus_valid;
  reg                 ulpi_domain_utmi_host_disconnect;
  reg                 ulpi_domain_utmi_id_dig;
  reg        [7:0]    ulpi_domain_reg_wr_data;
  reg        `UlpiState_binary_sequential_type ulpi_domain_cur_state;
  wire                ulpi_domain_reg_wr_ongoing;
  `ifndef SYNTHESIS
  reg [127:0] ulpi_domain_cur_state_string;
  `endif


  assign _zz_Utmi2Ulpi_1 = (! io_ulpi_direction);
  assign _zz_Utmi2Ulpi_2 = ((io_utmi_tx_valid && (io_utmi_op_mode == 2'b10)) && (io_utmi_data_in == 8'hff));
  assign _zz_Utmi2Ulpi_3 = io_ulpi_data_in[3 : 2];
  assign _zz_Utmi2Ulpi_4 = io_ulpi_data_in[5 : 4];
  `ifndef SYNTHESIS
  always @(*) begin
    case(ulpi_domain_cur_state)
      `UlpiState_binary_sequential_Idle : ulpi_domain_cur_state_string = "Idle            ";
      `UlpiState_binary_sequential_TxStart : ulpi_domain_cur_state_string = "TxStart         ";
      `UlpiState_binary_sequential_TxData : ulpi_domain_cur_state_string = "TxData          ";
      `UlpiState_binary_sequential_RegWrWaitAddrAck : ulpi_domain_cur_state_string = "RegWrWaitAddrAck";
      `UlpiState_binary_sequential_RegWrWaitDataAck : ulpi_domain_cur_state_string = "RegWrWaitDataAck";
      `UlpiState_binary_sequential_RegWrStp : ulpi_domain_cur_state_string = "RegWrStp        ";
      `UlpiState_binary_sequential_RegRdAddr : ulpi_domain_cur_state_string = "RegRdAddr       ";
      `UlpiState_binary_sequential_RegRdTurn : ulpi_domain_cur_state_string = "RegRdTurn       ";
      `UlpiState_binary_sequential_RegRdData : ulpi_domain_cur_state_string = "RegRdData       ";
      default : ulpi_domain_cur_state_string = "????????????????";
    endcase
  end
  `endif

  assign io_utmi_clk = io_ulpi_clk;
  assign ulpi_domain_turn_around = (io_ulpi_direction != io_ulpi_direction_regNext);
  assign ulpi_domain_func_ctrl_diff = ((((ulpi_domain_utmi_xcvr_select != io_utmi_xcvr_select) || (ulpi_domain_utmi_term_select != io_utmi_term_select)) || (ulpi_domain_utmi_op_mode != io_utmi_op_mode)) || (ulpi_domain_utmi_suspend_m != io_utmi_suspend_m));
  assign io_ulpi_data_ena = ((! ulpi_domain_turn_around) && (! io_ulpi_direction));
  assign io_ulpi_data_out = ulpi_domain_ulpi_data_out;
  assign io_ulpi_stp = ulpi_domain_ulpi_stp;
  assign ulpi_domain_utmi_rx_valid = 1'b0;
  assign ulpi_domain_utmi_rx_data = 8'h0;
  assign io_utmi_rx_active = ulpi_domain_utmi_rx_active;
  assign io_utmi_rx_error = ulpi_domain_utmi_rx_error;
  assign io_utmi_rx_valid = ulpi_domain_utmi_rx_valid;
  assign io_utmi_data_out = ulpi_domain_utmi_rx_data;
  assign io_utmi_tx_ready = ulpi_domain_utmi_tx_ready;
  assign io_utmi_line_state = ulpi_domain_utmi_line_state;
  assign io_ulpi_reset = 1'b0;
  assign ulpi_domain_reg_wr_ongoing = ((ulpi_domain_cur_state == `UlpiState_binary_sequential_RegWrWaitAddrAck) || (ulpi_domain_cur_state == `UlpiState_binary_sequential_RegWrWaitDataAck));
  always @ (*) begin
    ulpi_domain_utmi_tx_ready = 1'b0;
    if(! ulpi_domain_turn_around) begin
      if(! io_ulpi_direction) begin
        if(_zz_Utmi2Ulpi_1)begin
          case(ulpi_domain_cur_state)
            `UlpiState_binary_sequential_TxStart : begin
              ulpi_domain_utmi_tx_ready = 1'b1;
            end
            `UlpiState_binary_sequential_TxData : begin
              if(io_ulpi_nxt)begin
                if(! _zz_Utmi2Ulpi_2) begin
                  if(io_utmi_tx_valid)begin
                    ulpi_domain_utmi_tx_ready = 1'b1;
                  end else begin
                    ulpi_domain_utmi_tx_ready = 1'b1;
                  end
                end
              end
            end
            default : begin
            end
          endcase
        end
      end
    end
  end

  always @ (posedge io_ulpi_clk) begin
    ulpi_reset_ <= 1'b1;
  end

  always @ (posedge io_ulpi_clk) begin
    ulpi_domain_utmi_xcvr_select <= io_utmi_xcvr_select;
    ulpi_domain_utmi_term_select <= io_utmi_term_select;
    ulpi_domain_utmi_op_mode <= io_utmi_op_mode;
    ulpi_domain_utmi_suspend_m <= io_utmi_suspend_m;
    ulpi_domain_func_reset <= io_func_reset;
    io_ulpi_direction_regNext <= io_ulpi_direction;
    if(ulpi_domain_func_ctrl_diff)begin
      ulpi_domain_func_ctrl_wr_pending <= 1'b1;
    end
    if(! ulpi_domain_turn_around) begin
      if(! io_ulpi_direction) begin
        if(_zz_Utmi2Ulpi_1)begin
          case(ulpi_domain_cur_state)
            `UlpiState_binary_sequential_RegWrWaitDataAck : begin
              if(io_ulpi_nxt)begin
                if((ulpi_domain_func_ctrl_wr_pending && (! ulpi_domain_func_ctrl_diff)))begin
                  ulpi_domain_func_ctrl_wr_pending <= 1'b0;
                end
              end
            end
            default : begin
            end
          endcase
        end
      end
    end
  end

  always @ (posedge io_ulpi_clk or negedge ulpi_reset_) begin
    if (!ulpi_reset_) begin
      ulpi_domain_ulpi_data_out <= 8'h0;
      ulpi_domain_ulpi_stp <= 1'b1;
      ulpi_domain_utmi_rx_active <= 1'b0;
      ulpi_domain_utmi_rx_error <= 1'b0;
      ulpi_domain_utmi_line_state <= 2'b00;
      ulpi_domain_utmi_sess_end <= 1'b0;
      ulpi_domain_utmi_sess_valid <= 1'b0;
      ulpi_domain_utmi_vbus_valid <= 1'b0;
      ulpi_domain_utmi_host_disconnect <= 1'b0;
      ulpi_domain_utmi_id_dig <= 1'b0;
      ulpi_domain_reg_wr_data <= 8'h0;
      ulpi_domain_cur_state <= `UlpiState_binary_sequential_Idle;
    end else begin
      ulpi_domain_ulpi_stp <= 1'b0;
      if(ulpi_domain_turn_around)begin
        if((io_ulpi_direction && io_ulpi_nxt))begin
          if(ulpi_domain_reg_wr_ongoing)begin
            ulpi_domain_cur_state <= `UlpiState_binary_sequential_Idle;
          end
          ulpi_domain_utmi_rx_active <= 1'b1;
        end else begin
          if((! io_ulpi_direction))begin
            ulpi_domain_utmi_rx_active <= 1'b0;
          end
        end
      end else begin
        if(io_ulpi_direction)begin
          if((! io_ulpi_nxt))begin
            ulpi_domain_utmi_line_state <= io_ulpi_data_in[1 : 0];
            case(_zz_Utmi2Ulpi_3)
              2'b00 : begin
                ulpi_domain_utmi_sess_end <= 1'b1;
                ulpi_domain_utmi_sess_valid <= 1'b0;
                ulpi_domain_utmi_vbus_valid <= 1'b0;
              end
              2'b01 : begin
                ulpi_domain_utmi_sess_end <= 1'b0;
                ulpi_domain_utmi_sess_valid <= 1'b0;
                ulpi_domain_utmi_vbus_valid <= 1'b0;
              end
              2'b10 : begin
                ulpi_domain_utmi_sess_end <= 1'b0;
                ulpi_domain_utmi_sess_valid <= 1'b1;
                ulpi_domain_utmi_vbus_valid <= 1'b0;
              end
              default : begin
                ulpi_domain_utmi_sess_end <= 1'b0;
                ulpi_domain_utmi_sess_valid <= 1'b0;
                ulpi_domain_utmi_vbus_valid <= 1'b1;
              end
            endcase
            case(_zz_Utmi2Ulpi_4)
              2'b00 : begin
                ulpi_domain_utmi_rx_active <= 1'b0;
                ulpi_domain_utmi_rx_error <= 1'b0;
                ulpi_domain_utmi_host_disconnect <= 1'b0;
              end
              2'b01 : begin
                ulpi_domain_utmi_rx_active <= 1'b1;
                ulpi_domain_utmi_rx_error <= 1'b0;
                ulpi_domain_utmi_host_disconnect <= 1'b0;
              end
              2'b10 : begin
                ulpi_domain_utmi_rx_active <= 1'b1;
                ulpi_domain_utmi_rx_error <= 1'b1;
                ulpi_domain_utmi_host_disconnect <= 1'b0;
              end
              default : begin
                ulpi_domain_utmi_rx_active <= 1'b0;
                ulpi_domain_utmi_rx_error <= 1'b0;
                ulpi_domain_utmi_host_disconnect <= 1'b1;
              end
            endcase
            ulpi_domain_utmi_id_dig <= io_ulpi_data_in[6];
          end
        end else begin
          if(_zz_Utmi2Ulpi_1)begin
            case(ulpi_domain_cur_state)
              `UlpiState_binary_sequential_Idle : begin
                if(ulpi_domain_func_ctrl_wr_pending)begin
                  ulpi_domain_ulpi_data_out <= {2'b10,6'h04};
                  ulpi_domain_reg_wr_data <= {{{{{1'b0,ulpi_domain_utmi_suspend_m},ulpi_domain_func_reset},ulpi_domain_utmi_op_mode},ulpi_domain_utmi_term_select},ulpi_domain_utmi_xcvr_select};
                  ulpi_domain_cur_state <= `UlpiState_binary_sequential_RegWrWaitAddrAck;
                end else begin
                  if(io_utmi_tx_valid)begin
                    ulpi_domain_ulpi_data_out <= {{2'b01,2'b00},io_utmi_data_in[3 : 0]};
                    ulpi_domain_cur_state <= `UlpiState_binary_sequential_TxStart;
                  end
                end
              end
              `UlpiState_binary_sequential_TxStart : begin
                ulpi_domain_cur_state <= `UlpiState_binary_sequential_TxData;
              end
              `UlpiState_binary_sequential_TxData : begin
                if(io_ulpi_nxt)begin
                  if(_zz_Utmi2Ulpi_2)begin
                    ulpi_domain_ulpi_data_out <= io_utmi_data_in;
                    ulpi_domain_ulpi_stp <= 1'b1;
                    ulpi_domain_cur_state <= `UlpiState_binary_sequential_Idle;
                  end else begin
                    if(io_utmi_tx_valid)begin
                      ulpi_domain_ulpi_data_out <= io_utmi_data_in;
                      ulpi_domain_cur_state <= `UlpiState_binary_sequential_TxData;
                    end else begin
                      ulpi_domain_ulpi_data_out <= 8'h0;
                      ulpi_domain_ulpi_stp <= 1'b1;
                      ulpi_domain_cur_state <= `UlpiState_binary_sequential_Idle;
                    end
                  end
                end
              end
              `UlpiState_binary_sequential_RegWrWaitAddrAck : begin
                if(io_ulpi_nxt)begin
                  ulpi_domain_ulpi_data_out <= ulpi_domain_reg_wr_data;
                  ulpi_domain_cur_state <= `UlpiState_binary_sequential_RegWrWaitDataAck;
                end
              end
              `UlpiState_binary_sequential_RegWrWaitDataAck : begin
                if(io_ulpi_nxt)begin
                  ulpi_domain_ulpi_stp <= 1'b1;
                  ulpi_domain_ulpi_data_out <= 8'h0;
                  ulpi_domain_cur_state <= `UlpiState_binary_sequential_Idle;
                end
              end
              default : begin
              end
            endcase
          end
        end
      end
    end
  end


endmodule

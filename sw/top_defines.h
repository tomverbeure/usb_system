
#ifndef TOP_DEFINES_H
#define TOP_DEFINES_H

//============================================================
// Timer
//============================================================

//== TIMER PRESCALER

#define TIMER_PRESCALER_LIMIT_ADDR  0x00000000

#define TIMER_PRESCALER_LIMIT_VALUE_FIELD_START         0
#define TIMER_PRESCALER_LIMIT_VALUE_FIELD_LENGTH        16

//== TIMER IRQ

#define TIMER_IRQ_STATUS_ADDR       0x00000010

#define TIMER_IRQ_STATUS_PENDING_FIELD_START            0
#define TIMER_IRQ_STATUS_PENDING_FIELD_LENGTH           2

#define TIMER_IRQ_MASK_ADDR         0x00000014

#define TIMER_IRQ_MASK_VALUE_FIELD_START                0
#define TIMER_IRQ_MASK_VALUE_FIELD_LENGTH               2

//== TIMERS

#define TIMER_A_CONFIG_ADDR         0x00000040

#define TIMER_A_CONFIG_TICKS_ENABLE_FIELD_START         0
#define TIMER_A_CONFIG_TICKS_ENABLE_FIELD_LENGTH        16

#define TIMER_A_CONFIG_CLEARS_ENABLE_FIELD_START        16
#define TIMER_A_CONFIG_CLEARS_ENABLE_FIELD_LENGTH       16

#define TIMER_A_LIMIT_ADDR          0x00000044

#define TIMER_A_LIMIT_VALUE_FIELD_START                 0
#define TIMER_A_LIMIT_VALUE_FIELD_LENGTH                32

#define TIMER_A_VALUE_ADDR          0x00000048

#define TIMER_A_VALUE_VALUE_FIELD_START                 0
#define TIMER_A_VALUE_VALUE_FIELD_LENGTH                32

#define TIMER_B_CONFIG_ADDR         0x00000050

#define TIMER_B_CONFIG_TICKS_ENABLE_FIELD_START         0
#define TIMER_B_CONFIG_TICKS_ENABLE_FIELD_LENGTH        16

#define TIMER_B_CONFIG_CLEARS_ENABLE_FIELD_START        16
#define TIMER_B_CONFIG_CLEARS_ENABLE_FIELD_LENGTH       16

#define TIMER_B_LIMIT_ADDR          0x00000054

#define TIMER_B_LIMIT_VALUE_FIELD_START                 0
#define TIMER_B_LIMIT_VALUE_FIELD_LENGTH                32

#define TIMER_B_VALUE_ADDR          0x00000058

#define TIMER_B_VALUE_VALUE_FIELD_START                 0
#define TIMER_B_VALUE_VALUE_FIELD_LENGTH                32

//============================================================
// LEDs
//============================================================

#define LED_READ_ADDR               0x00010000
#define LED_WRITE_ADDR              0x00010004
#define	LED_DIR_ADDR                0x00010008

//============================================================
// Misc Control and Status
//============================================================
//
#define MISC_CTRL_ADDR              0x00011000

#define MISC_STATUS_ADDR              0x00011004

#define MISC_STATUS_HAS_JTAG_UART_FIELD_START           0
#define MISC_STATUS_HAS_JTAG_UART_FIELD_LENGTH          1

#define MISC_STATUS_HAS_UART_FIELD_START                1
#define MISC_STATUS_HAS_UART_FIELD_LENGTH               1

#define MISC_STATUS_IS_SIM_FIELD_START                  2
#define MISC_STATUS_IS_SIM_FIELD_LENGTH                 1

#define MISC_STATUS_HAS_ULPI_PLL_FIELD_START            3
#define MISC_STATUS_HAS_ULPI_PLL_FIELD_LENGTH           1

//============================================================
// JTAG UART
//============================================================

#define JTAG_UART_DATA_ADDR         0x00013000

#define JTAG_UART_DATA_DATA_FIELD_START                         0
#define JTAG_UART_DATA_DATA_FIELD_LENGTH                        8

#define JTAG_UART_DATA_RVALID_FIELD_START                       15
#define JTAG_UART_DATA_RVALID_FIELD_LENGTH                      1

#define JTAG_UART_DATA_RAVAIL_FIELD_START                       16
#define JTAG_UART_DATA_RAVAIL_FIELD_LENGTH                      16

#define JTAG_UART_CONTROL_ADDR      0x00013004

#define JTAG_UART_CONTROL_RE_FIELD_START                        0
#define JTAG_UART_CONTROL_RE_FIELD_LENGTH                       1

#define JTAG_UART_CONTROL_WE_FIELD_START                        1
#define JTAG_UART_CONTROL_WE_FIELD_LENGTH                       1

#define JTAG_UART_CONTROL_RI_FIELD_START                        8
#define JTAG_UART_CONTROL_WI_FIELD_LENGTH                       1

#define JTAG_UART_CONTROL_AC_FIELD_START                        9
#define JTAG_UART_CONTROL_AC_FIELD_LENGTH                       1

#define JTAG_UART_CONTROL_WSPACE_FIELD_START                    16
#define JTAG_UART_CONTROL_WSPACE_FIELD_LENGTH                   16

//============================================================
// UART
//============================================================

#define UART_RXTX_ADDR              0x00015000

#define UART_RXTX_DATA_FIELD_START                      0
#define UART_RXTX_DATA_FIELD_LENGTH                     8

#define UART_RXTX_RX_HAS_DATA_FIELD_START               16
#define UART_RXTX_RX_HAS_DATA_FIELD_LENGTH              0

#define UART_STATUS_ADDR            0x00015004

#define UART_STATUS_TX_AVAIL_FIELD_START           16
#define UART_STATUS_TX_AVAIL_FIELD_LENGTH          8

#define UART_CLK_DIV_ADDR           0x00015008

#define UART_CLK_DIV_DIVIDER_FIELD_START                0
#define UART_CLK_DIV_DIVIDER_FIELD_LENGTH               16

#define UART_FRAME_ADDR             0x0001500c

#define UART_FRAME_DATA_LENGTH_FIELD_START              0
#define UART_FRAME_DATA_LENGTH_FIELD_LENGTH             3

#define UART_FRAME_PARITY_FIELD_START                   8
#define UART_FRAME_PARITY_FIELD_LENGTH                  2

#define UART_FRAME_STOP_FIELD_START                     16
#define UART_FRAME_STOP_FIELD_LENGTH                    1

//============================================================
// UTMI2ULPI
//============================================================

#define UTMI2ULPI_CONFIG_ADDR       0x00016000

#define UTMI2ULPI_CONFIG_ENABLE_FIELD_START                     0
#define UTMI2ULPI_CONFIG_ENABLE_FIELD_LENGTH                    1

#define UTMI2ULPI_CONFIG_FUNC_RESET_FIELD_START                 1
#define UTMI2ULPI_CONFIG_FUNC_RESET_FIELD_LENGTH                1

#define UTMI2ULPI_STATUS_ADDR       0x00016004

#define UTMI2ULPI_STATUS_PLL_LOCKED_FIELD_START                 0
#define UTMI2ULPI_STATUS_PLL_LOCKED_FIELD_LENGTH                1

#define UTMI2ULPI_REG_ADDR          0x00016008

#define UTMI2ULPI_REG_REQ_FIELD_START                           0
#define UTMI2ULPI_REG_REQ_FIELD_LENGTH                          1

#define UTMI2ULPI_REG_REQ_DONE_FIELD_START                      1
#define UTMI2ULPI_REG_REQ_DONE_FIELD_LENGTH                     1

#define UTMI2ULPI_REG_RD_FIELD_START                            2
#define UTMI2ULPI_REG_RD_FIELD_LENGTH                           1

#define UTMI2ULPI_REG_ADDR_FIELD_START                          8
#define UTMI2ULPI_REG_ADDR_FIELD_LENGTH                         8

#define UTMI2ULPI_REG_WDATA_FIELD_START                         16
#define UTMI2ULPI_REG_WDATA_FIELD_LENGTH                        8

#define UTMI2ULPI_REG_RDATA_FIELD_START                         24
#define UTMI2ULPI_REG_RDATA_FIELD_LENGTH                        8

//============================================================
// USB DEVICE
//============================================================
//
#define USB_DEV_CONFIG_ADDR            0x00020000

#define USB_DEV_CONFIG_ENABLE_HS_FIELD_START                        0
#define USB_DEV_CONFIG_ENABLE_HS_FIELD_LENGTH                       1

#define USB_DEV_STATUS_ADDR            0x00020004

#define USB_DEV_STATUS_SOF_FRAME_NR_FIELD_START                     0
#define USB_DEV_STATUS_SOF_FRAME_NR_FIELD_LENGTH                    1

#define USB_DEV_STATE_ADDR             0x00020008

#define USB_DEV_STATE_DEV_FSM_STATE_FIELD_START                     0
#define USB_DEV_STATE_DEV_FSM_STATE_FIELD_LENGTH                    4

#define USB_DEV_DEBUG_ADDR             0x00020010

#define USB_DEV_DEBUG_FORCE_TERM_SELECT_FIELD_START                 0
#define USB_DEV_DEBUG_FORCE_TERM_SELECT_FIELD_LENGTH                1

#define USB_DEV_DEBUG_FORCE_TERM_SELECT_VALUE_FIELD_START           1
#define USB_DEV_DEBUG_FORCE_TERM_SELECT_VALUE_FIELD_LENGTH          1

#define USB_DEV_DEBUG_FORCE_SUSPEND_M_FIELD_START                   2
#define USB_DEV_DEBUG_FORCE_SUSPEND_M_FIELD_LENGTH                  1

#define USB_DEV_DEBUG_FORCE_SUSPEND_M_VALUE_FIELD_START             3
#define USB_DEV_DEBUG_FORCE_SUSPEND_M_VALUE_FIELD_LENGTH            1

#define USB_DEV_DEBUG_FORCE_XCVR_SELECT_FIELD_START                 4
#define USB_DEV_DEBUG_FORCE_XCVR_SELECT_FIELD_LENGTH                1

#define USB_DEV_DEBUG_FORCE_XCVR_SELECT_VALUE_FIELD_START           5
#define USB_DEV_DEBUG_FORCE_XCVR_SELECT_VALUE_FIELD_LENGTH          2

#define USB_DEV_DEBUG_FORCE_OP_MODE_FIELD_START                     7
#define USB_DEV_DEBUG_FORCE_OP_MODE_FIELD_LENGTH                    1

#define USB_DEV_DEBUG_FORCE_OP_MODE_VALUE_FIELD_START               8
#define USB_DEV_DEBUG_FORCE_OP_MODE_VALUE_FIELD_LENGTH              2

//============================================================
// ONCHIP FLASH
//============================================================

#define ONCHIP_FLASH_DATA_ADDR              0x00200000

#define ONCHIP_FLASH_CSR_STATUS_ADDR        0x00280000

#define ONCHIP_FLASH_CSR_STATUS_BUSY_FIELD_START               	0
#define ONCHIP_FLASH_CSR_STATUS_BUSY_FIELD_LENGTH              	2

#define ONCHIP_FLASH_CSR_STATUS_RS_FIELD_START               	2
#define ONCHIP_FLASH_CSR_STATUS_RS_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_STATUS_WS_FIELD_START               	3
#define ONCHIP_FLASH_CSR_STATUS_WS_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_STATUS_ES_FIELD_START               	4
#define ONCHIP_FLASH_CSR_STATUS_ES_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_STATUS_SP1_FIELD_START               	5
#define ONCHIP_FLASH_CSR_STATUS_SP1_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_STATUS_SP2_FIELD_START               	6
#define ONCHIP_FLASH_CSR_STATUS_SP2_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_STATUS_SP3_FIELD_START               	7
#define ONCHIP_FLASH_CSR_STATUS_SP3_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_STATUS_SP4_FIELD_START               	8
#define ONCHIP_FLASH_CSR_STATUS_SP4_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_STATUS_SP5_FIELD_START               	9
#define ONCHIP_FLASH_CSR_STATUS_SP5_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_CONTROL_ADDR       0x00280004

#define ONCHIP_FLASH_CSR_CONTROL_PE_FIELD_START               	0
#define ONCHIP_FLASH_CSR_CONTROL_PE_FIELD_LENGTH              	20

#define ONCHIP_FLASH_CSR_CONTROL_SE_FIELD_START               	20
#define ONCHIP_FLASH_CSR_CONTROL_SE_FIELD_LENGTH              	3

#define ONCHIP_FLASH_CSR_CONTROL_WP1_FIELD_START               	23
#define ONCHIP_FLASH_CSR_CONTROL_WP1_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_CONTROL_WP2_FIELD_START               	24
#define ONCHIP_FLASH_CSR_CONTROL_WP2_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_CONTROL_WP3_FIELD_START               	25
#define ONCHIP_FLASH_CSR_CONTROL_WP3_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_CONTROL_WP4_FIELD_START               	26
#define ONCHIP_FLASH_CSR_CONTROL_WP4_FIELD_LENGTH              	1

#define ONCHIP_FLASH_CSR_CONTROL_WP5_FIELD_START               	27
#define ONCHIP_FLASH_CSR_CONTROL_WP5_FIELD_LENGTH              	1

#endif




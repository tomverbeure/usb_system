#include <stdint.h>
#include <stdlib.h>
#include <math.h>

#include "reg.h"
#include "top_defines.h"
#include "lib.h"
#include "ulpi.h"

#include "jtag_uart.h"

int is_sim = 0;

void trap()
{
}

uint8_t ulpi_reg_rd(uint8_t addr)
{
    REG_WR_FIELD(UTMI2ULPI_REG, REQ,    0);

    REG_WR_FIELD(UTMI2ULPI_REG, RD,     1);
    REG_WR_FIELD(UTMI2ULPI_REG, ADDR,   addr);
    REG_WR_FIELD(UTMI2ULPI_REG, WDATA,  0);

    REG_WR_FIELD(UTMI2ULPI_REG, REQ,    1);
    while(!REG_RD_FIELD(UTMI2ULPI_REG, REQ_DONE))
        ;
    REG_WR_FIELD(UTMI2ULPI_REG, REQ,    0);

    return REG_RD_FIELD(UTMI2ULPI_REG, RDATA);
}

void ulpi_reg_wr(uint8_t addr, uint8_t wdata)
{
    REG_WR_FIELD(UTMI2ULPI_REG, REQ,    0);

    REG_WR_FIELD(UTMI2ULPI_REG, RD,     0);
    REG_WR_FIELD(UTMI2ULPI_REG, ADDR,   addr);
    REG_WR_FIELD(UTMI2ULPI_REG, WDATA,  wdata);

    REG_WR_FIELD(UTMI2ULPI_REG, REQ,    1);
    while(!REG_RD_FIELD(UTMI2ULPI_REG, REQ_DONE))
        ;
    REG_WR_FIELD(UTMI2ULPI_REG, REQ,    0);
}

int ulpi_pll_locked()
{
    return REG_RD_FIELD(UTMI2ULPI_STATUS, PLL_LOCKED);
}

uint16_t get_vendor_id()
{
    uint16_t vendor_id = (ulpi_reg_rd(0x01) << 8) | ulpi_reg_rd(0x00); 
    return vendor_id;
}

uint16_t get_product_id()
{
    uint16_t product_id = (ulpi_reg_rd(0x03) << 8) | ulpi_reg_rd(0x02); 
    return product_id;
}

void cmdline()
{
    static uint8_t scratch_val = 0x5a;

    int ret;
    do{
        unsigned char c;
        ret = jtag_uart_rx_get_char(&c);
        if (ret != 0){
            jtag_uart_tx_str("Command: ");
            jtag_uart_tx_char(c);
            jtag_uart_tx_char('\n');

            switch(c){
                case 't': {
                    jtag_uart_tx_str("term_select toggle\n");
                    int term_select = REG_RD_FIELD(USB_DEV_DEBUG, FORCE_TERM_SELECT_VALUE);
                    term_select ^= 1;
                    jtag_uart_tx_dec(term_select);
                    REG_WR_FIELD(USB_DEV_DEBUG, FORCE_TERM_SELECT_VALUE, term_select);
                    REG_WR_FIELD(USB_DEV_DEBUG, FORCE_TERM_SELECT, 1);
                    break;
                }
                case 'x': {
                    jtag_uart_tx_str("xcvr_select toggle\n");
                    int xcvr_select = REG_RD_FIELD(USB_DEV_DEBUG, FORCE_XCVR_SELECT_VALUE);
                    xcvr_select ^= 1;
                    jtag_uart_tx_dec(xcvr_select);
                    REG_WR_FIELD(USB_DEV_DEBUG, FORCE_XCVR_SELECT_VALUE, xcvr_select);
                    REG_WR_FIELD(USB_DEV_DEBUG, FORCE_XCVR_SELECT, 1);
                    break;
                }
                case 'r': {
                    jtag_uart_tx_str("read scratch\n");
                    uint8_t val = ulpi_reg_rd(ULPI_SCRATCH_ADDR);
                    jtag_uart_tx_byte(val);
                    jtag_uart_tx_str("\n");
                    scratch_val += 1;

                    jtag_uart_tx_str("OTG control\n");
                    val = ulpi_reg_rd(ULPI_OTG_CONTROL_ADDR);
                    jtag_uart_tx_byte(val);
                    jtag_uart_tx_str("\n");
                    break;
                }
                case 'w': {
                    jtag_uart_tx_str("write scratch\n");
                    jtag_uart_tx_byte(scratch_val);
                    jtag_uart_tx_str("\n");
                    ulpi_reg_wr(ULPI_SCRATCH_ADDR, scratch_val);
                    break;
                }
                case 'p': {
                    jtag_uart_tx_str("vendor_id : ");
                    jtag_uart_tx_short(get_vendor_id());
                    jtag_uart_tx_str("\nproduct_id: ");
                    jtag_uart_tx_short(get_product_id());
                    jtag_uart_tx_str("\n");
                }

                default:
                    break;
            }
        }
    } while(ret != 0);

}

int main() 
{
    is_sim = REG_RD_FIELD(MISC_STATUS, IS_SIM);

    const char hello[] = "Hello World!\n";

    jtag_uart_init();
    jtag_uart_tx_str(hello);

    if (REG_RD_FIELD(MISC_STATUS, HAS_ULPI_PLL)){
        while(!ulpi_pll_locked())
            ;
        jtag_uart_tx_str("PLL locked.\n");
    }

    if (!is_sim)
        wait_ms(5000);

    // Kick off overall ULPI FSM
    jtag_uart_tx_str("ULPI enable.\n");
    REG_WR_FIELD(UTMI2ULPI_CONFIG, ENABLE, 1);

    get_vendor_id();
    get_product_id();

    ulpi_reg_rd(ULPI_OTG_CONTROL_ADDR);

    // Disable pulldown resistors.
    ulpi_reg_wr(ULPI_OTG_CONTROL_ADDR, 0x00);
    ulpi_reg_rd(ULPI_OTG_CONTROL_ADDR);

    REG_WR_FIELD(UTMI2ULPI_CONFIG, FUNC_RESET, 1);
    REG_WR_FIELD(UTMI2ULPI_CONFIG, FUNC_RESET, 0);

    REG_WR(LED_DIR, 0xff);

    while(1){
        cmdline();

        REG_WR(LED_WRITE, 0x00);
        wait_ms(is_sim ? 0 : 300);
        REG_WR(LED_WRITE, 0xff);
        wait_ms(is_sim ? 0 : 100);
    }
}

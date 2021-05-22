#include <stdint.h>
#include <stdlib.h>
#include <math.h>

#include "reg.h"
#include "top_defines.h"
#include "lib.h"

#include "jtag_uart.h"

void trap()
{
}

void cmdline()
{
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

                default:
                    break;
            }
        }
    } while(ret != 0);

}

int main() 
{
    const char hello[] = "Hello World!\n";

    jtag_uart_init();
    jtag_uart_tx_str(hello);

    REG_WR(LED_DIR, 0xff);

    while(1){
        cmdline();

        REG_WR(LED_WRITE, 0x00);
        wait_ms(300);
        REG_WR(LED_WRITE, 0xff);
        wait_ms(100);
    }
}

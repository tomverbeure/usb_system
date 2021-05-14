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

int main() 
{
    const char hello[] = "Hello World!\n";

    jtag_uart_init();
    jtag_uart_tx_str(hello);

    REG_WR(LED_DIR, 0xff);

    while(1){
        REG_WR(LED_WRITE, 0x00);
        wait_ms(300);
        REG_WR(LED_WRITE, 0xff);
        wait_ms(100);
    }
}

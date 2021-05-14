#include <stdint.h>
#include <math.h>

#include "jtag_uart.h"

#include "top_defines.h"
#include "reg.h"

#if 0
// uart not enabled if in production.
void jtag_uart_init(void) {}
uint32_t jtag_uart_rx_get_char(uint8_t *pChar) { *pChar = 0U; return 0UL; }
#endif

static const unsigned char hex2char[16] = "0123456789abcdef";
#if 1
void jtag_uart_init(void)
{
}

static inline int has_jtag_uart()
{
    return REG_RD_FIELD(MISC_STATUS, HAS_JTAG_UART);
}

void jtag_uart_tx_char(const char c)
{
    uint32_t val;

    if (!has_jtag_uart())
        return;

    while((val = REG_RD_FIELD(JTAG_UART_CONTROL, WSPACE)) < 1);
    REG_WR(JTAG_UART_DATA, c);
}

void jtag_uart_tx_str(const char *str)
{
    if (!has_jtag_uart())
        return;

    while(*str != 0){
        jtag_uart_tx_char(*str);
        ++str;
    }
}

void jtag_uart_tx_byte(uint8_t b)
{
    if (!has_jtag_uart())
        return;

    jtag_uart_tx_char(hex2char[b>>4]);
    jtag_uart_tx_char(hex2char[b&0xf]);
}

void jtag_uart_tx_short(uint16_t c)
{
    if (!has_jtag_uart())
        return;

    uint8_t b;

    b = (uint8_t)((c>> 8) & 0xff);
    jtag_uart_tx_char(hex2char[b>>4]);
    jtag_uart_tx_char(hex2char[b&0xf]);

    b = (uint8_t)(c & 0xff);
    jtag_uart_tx_char(hex2char[b>>4]);
    jtag_uart_tx_char(hex2char[b&0xf]);

}

void jtag_uart_tx_long(uint32_t c)
{
    if (!has_jtag_uart())
        return;

    uint16_t a;

    a = (uint16_t)(c >> 16);
    jtag_uart_tx_short(a);
    a = (uint16_t)(c & 0xffff);
    jtag_uart_tx_short(a);
}

void jtag_uart_tx_dec(int32_t val)
{
    if (!has_jtag_uart())
        return;

    if (val<0){
        jtag_uart_tx_char('-');
        val *= -1;
    }

    int non_zero_seen = 0;
    int div = 1000000000;
    for(int i=9;i>=0;i--){
        int digit = (val / div) % 10; 
        div /= 10;
        if (!non_zero_seen){
            if (digit != 0){
                non_zero_seen = 1;
            }
            else{
                continue;
            }
        }
        jtag_uart_tx_char(digit + '0');
    }

    if (!non_zero_seen){
        jtag_uart_tx_char('0');
    }
}

void jtag_uart_tx_wait_avail(__attribute__((unused))uint32_t nr_chars)
{
    //while(REG_RD_FIELD(JTAG_UART_CONTROL, WSPACE) < nr_chars);
}

uint32_t jtag_uart_rx_get_char(uint8_t *pChar)
{
    if (!has_jtag_uart())
        return 0;

    if (REG_RD_FIELD(JTAG_UART_DATA, RVALID)){
        *pChar = REG_RD_FIELD(JTAG_UART_DATA, DATA);
        return 1UL;
    }
    else{
        return 0UL;
    }
}
#endif



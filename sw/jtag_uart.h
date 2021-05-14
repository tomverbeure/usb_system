#ifndef JTAG_UART_H
#define JTAG_UART_H

void jtag_uart_init(void);

#if 1
void jtag_uart_tx_char(const char c);
void jtag_uart_tx_str(const char *str);
void jtag_uart_tx_byte(uint8_t b);
void jtag_uart_tx_short(uint16_t c);
void jtag_uart_tx_long(uint32_t c);
void jtag_uart_tx_dec(int32_t val);
void jtag_uart_tx_wait_avail(uint32_t nr_chars);
#else
#define jtag_uart_tx_char(...)
#define jtag_uart_tx_str(...)
#define jtag_uart_tx_byte(...)
#define jtag_uart_tx_short(...)
#define jtag_uart_tx_long(...)
#define jtag_uart_tx_dec(...)
#define jtag_uart_tx_wait_avail(...)
#endif

uint32_t jtag_uart_rx_get_char(uint8_t *pChar);

#endif

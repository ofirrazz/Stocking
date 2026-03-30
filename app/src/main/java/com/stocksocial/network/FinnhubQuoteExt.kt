package com.stocksocial.network

/** Current price: live trade when available, otherwise previous close (Finnhub often sends c=0 when halted). */
fun FinnhubQuoteDto.currentDisplayPrice(): Double? = when {
    c > 0 -> c
    pc > 0 -> pc
    else -> null
}

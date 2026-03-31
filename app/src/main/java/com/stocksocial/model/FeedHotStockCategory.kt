package com.stocksocial.model

/**
 * Preferred display order for Finnhub symbols shown in the feed "hot" row per chip.
 * Crypto uses exchange-pair symbols (see Finnhub quote API).
 */
object FeedHotStockCategory {

    val defaultTrendingOrder = listOf("NVDA", "AAPL", "TSLA", "MSFT", "GOOGL", "AMD")

    val technology = listOf("NVDA", "MSFT", "AAPL", "GOOGL", "META", "AMD")

    val banking = listOf("JPM", "BAC", "WFC", "GS", "C", "MS")

    val crypto = listOf(
        "BINANCE:BTCUSDT",
        "BINANCE:ETHUSDT",
        "BINANCE:SOLUSDT",
        "BINANCE:XRPUSDT",
        "BINANCE:ADAUSDT"
    )
}

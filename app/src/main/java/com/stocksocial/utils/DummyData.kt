package com.stocksocial.utils

import com.stocksocial.model.Stock
import com.stocksocial.model.StockSignal

object DummyData {

    fun watchlistStocks(): List<Stock> = listOf(
        Stock(symbol = "AAPL", name = "Apple", price = 227.45, dailyChangePercent = 1.14),
        Stock(symbol = "NVDA", name = "NVIDIA", price = 912.60, dailyChangePercent = 2.48),
        Stock(symbol = "MSFT", name = "Microsoft", price = 431.90, dailyChangePercent = 0.67),
        Stock(symbol = "TSLA", name = "Tesla", price = 188.30, dailyChangePercent = -1.21)
    )

    fun marketIndices(): List<Stock> = listOf(
        Stock(symbol = "SPY", name = "S&P 500 ETF", price = 523.81, dailyChangePercent = 0.52),
        Stock(symbol = "DIA", name = "Dow Jones ETF", price = 392.44, dailyChangePercent = -0.17),
        Stock(symbol = "QQQ", name = "Nasdaq 100 ETF", price = 449.26, dailyChangePercent = 0.93)
    )

    fun trendingStocks(): List<Stock> = listOf(
        Stock(symbol = "AAPL", name = "Apple", price = 227.45, dailyChangePercent = 1.14),
        Stock(symbol = "MSFT", name = "Microsoft", price = 431.90, dailyChangePercent = 0.67),
        Stock(symbol = "NVDA", name = "NVIDIA", price = 912.60, dailyChangePercent = 2.48),
        Stock(symbol = "AMD", name = "Advanced Micro Devices", price = 176.23, dailyChangePercent = 1.92),
        Stock(symbol = "TSLA", name = "Tesla", price = 188.30, dailyChangePercent = -1.21)
    )

    fun topSignals(): List<StockSignal> = listOf(
        StockSignal(
            symbol = "AAPL",
            ideaTitle = "Breakout above consolidation range",
            confidence = "High",
            currentPrice = 227.45,
            targetPrice = 239.00,
            trend = "Bullish"
        ),
        StockSignal(
            symbol = "MSFT",
            ideaTitle = "Support bounce with volume confirmation",
            confidence = "Medium",
            currentPrice = 431.90,
            targetPrice = 446.50,
            trend = "Bullish"
        ),
        StockSignal(
            symbol = "TSLA",
            ideaTitle = "Potential downside if 185 fails",
            confidence = "Medium",
            currentPrice = 188.30,
            targetPrice = 176.00,
            trend = "Bearish"
        )
    )
}

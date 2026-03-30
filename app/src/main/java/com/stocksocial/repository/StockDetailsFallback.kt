package com.stocksocial.repository

import com.stocksocial.model.AnalystRecommendation
import com.stocksocial.model.PriceChartSeries
import com.stocksocial.model.Stock
import kotlin.math.abs
import kotlin.math.sin

/**
 * Demo / offline data when Finnhub limits, network errors, or candle response is empty.
 */
object StockDetailsFallback {

    private val basePrices = mapOf(
        "AAPL" to 227.0,
        "NVDA" to 912.0,
        "MSFT" to 432.0,
        "TSLA" to 188.0,
        "GOOGL" to 175.0,
        "AMD" to 165.0,
        "META" to 520.0,
        "AMZN" to 185.0,
        "SPY" to 524.0,
        "QQQ" to 449.0,
        "DIA" to 392.0
    )

    fun basePrice(symbol: String): Double =
        basePrices[symbol.uppercase()] ?: (180.0 + abs(symbol.uppercase().hashCode() % 80))

    fun mockStock(symbol: String): Stock {
        val s = symbol.trim().uppercase()
        val base = basePrice(s)
        val pct = 0.85
        val delta = base * pct / 100.0
        return Stock(
            symbol = s,
            name = StockDetailsRepository.symbolNameOrTicker(s),
            price = base,
            dailyChangePercent = pct,
            marketCap = null,
            volume = 52_400_000L,
            open = base - delta * 0.3,
            high = base + delta * 0.5,
            low = base - delta * 0.7,
            previousClose = base - delta * 0.2,
            dayChangeAbs = delta
        )
    }

    fun mockChart(symbol: String): PriceChartSeries {
        val s = symbol.trim().uppercase()
        val base = basePrice(s)
        val nowSec = System.currentTimeMillis() / 1000
        val points = (0 until 30).map { i ->
            val t = nowSec - (29 - i) * 86400L
            val wave = sin(i * 0.35) * base * 0.012
            val drift = (i - 15) * base * 0.0018
            t to (base + drift + wave).coerceAtLeast(base * 0.92)
        }
        return PriceChartSeries(points = points, lastVolume = 48_000_000L)
    }

    fun enrichChartIfThin(series: PriceChartSeries, symbol: String): PriceChartSeries =
        if (series.points.size >= 2) series else mockChart(symbol)

    fun mockRecommendations(): List<AnalystRecommendation> = listOf(
        AnalystRecommendation(
            period = "2026-03 (mock)",
            strongBuy = 9,
            buy = 14,
            hold = 6,
            sell = 1,
            strongSell = 0
        ),
        AnalystRecommendation(
            period = "2026-02 (mock)",
            strongBuy = 8,
            buy = 12,
            hold = 8,
            sell = 2,
            strongSell = 0
        )
    )
}

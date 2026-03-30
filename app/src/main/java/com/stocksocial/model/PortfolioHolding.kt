package com.stocksocial.model

data class PortfolioHolding(
    val symbol: String,
    val shares: Double,
    val buyPrice: Double,
    val currentPrice: Double
) {
    val investedValue: Double get() = shares * buyPrice
    val currentValue: Double get() = shares * currentPrice
    val pnlValue: Double get() = currentValue - investedValue
    val pnlPercent: Double get() = if (investedValue == 0.0) 0.0 else (pnlValue / investedValue) * 100.0
}

package com.stocksocial.model

data class StockSignal(
    val symbol: String,
    val ideaTitle: String,
    val confidence: String,
    val currentPrice: Double,
    val targetPrice: Double,
    val trend: String
)

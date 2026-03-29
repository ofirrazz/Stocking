package com.stocksocial.model

data class Stock(
    val symbol: String,
    val name: String,
    val price: Double,
    val dailyChangePercent: Double,
    val marketCap: Long? = null,
    val volume: Long? = null
)

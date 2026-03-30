package com.stocksocial.model

data class PriceChartSeries(
    val points: List<Pair<Long, Double>> = emptyList(),
    val lastVolume: Long? = null
)

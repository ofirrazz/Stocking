package com.stocksocial.network

data class FinnhubCandleDto(
    val c: List<Double> = emptyList(),
    val t: List<Long> = emptyList(),
    val v: List<Long> = emptyList(),
    val s: String = ""
)

package com.stocksocial.model

data class AnalystRecommendation(
    val period: String,
    val buy: Int,
    val hold: Int,
    val sell: Int,
    val strongBuy: Int,
    val strongSell: Int
)

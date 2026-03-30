package com.stocksocial.network

data class FinnhubRecommendationDto(
    val period: String,
    val buy: Int,
    val hold: Int,
    val sell: Int,
    val strongBuy: Int,
    val strongSell: Int
)

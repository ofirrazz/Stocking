package com.stocksocial.model

data class WatchlistItem(
    val id: String,
    val userId: String,
    val symbol: String,
    val addedAt: Long
)

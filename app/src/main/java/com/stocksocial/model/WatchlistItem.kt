package com.stocksocial.model

data class WatchlistItem(
    val id: String,
    val userId: String,
    val stock: Stock,
    val createdAt: String
)

package com.stocksocial.model

data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val category: String? = null,
    val imageUrl: String? = null,
    val publishedAt: Long? = null
)

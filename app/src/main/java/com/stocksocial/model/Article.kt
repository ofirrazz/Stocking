package com.stocksocial.model

data class Article(
    val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val author: String,
    val content: String,
    val source: String,
    val imageUrl: String? = null,
    val publishedAt: String? = null,
    val url: String? = null,
    val localImagePath: String? = null
)

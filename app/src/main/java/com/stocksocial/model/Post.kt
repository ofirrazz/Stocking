package com.stocksocial.model

data class Post(
    val id: String,
    val author: User,
    val content: String,
    val createdAt: String,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val stockSymbol: String? = null,
    val stockPrice: Double? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val localImagePath: String? = null
)

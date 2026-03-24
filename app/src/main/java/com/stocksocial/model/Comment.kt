package com.stocksocial.model

data class Comment(
    val id: String,
    val postId: String,
    val author: User,
    val content: String,
    val createdAt: String
)

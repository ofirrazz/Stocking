package com.stocksocial.model

data class PostComment(
    val id: String,
    val authorUsername: String,
    val content: String,
    val createdAt: String
)

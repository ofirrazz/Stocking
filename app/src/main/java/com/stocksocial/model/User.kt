package com.stocksocial.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val imageUrl: String? = null,
    val bio: String? = null
)

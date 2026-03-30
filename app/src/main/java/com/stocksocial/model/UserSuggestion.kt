package com.stocksocial.model

data class UserSuggestion(
    val id: String,
    val username: String,
    val avatarUrl: String? = null
)

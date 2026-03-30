package com.stocksocial.model

data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val displayName: String? = null,
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val totalLikesReceived: Int = 0
)

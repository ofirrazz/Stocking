package com.stocksocial.model

data class PublicUserProfile(
    val userId: String,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val location: String?,
    val website: String?,
    val joinedTimestamp: Long?,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int = 0,
    val totalLikesReceived: Int = 0
)

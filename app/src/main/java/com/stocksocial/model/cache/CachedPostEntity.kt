package com.stocksocial.model.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class CachedPostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorUsername: String,
    val content: String,
    val createdAt: String,
    val createdAtMillis: Long,
    val likesCount: Int,
    val likedByCurrentUser: Boolean,
    val commentsCount: Int,
    val stockSymbol: String?,
    val stockPrice: Double?,
    val imageUrl: String?,
    val videoUrl: String?,
    val localImagePath: String?
)

package com.stocksocial.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val username: String,
    val content: String,
    val createdAt: String,
    val likesCount: Int,
    val commentsCount: Int,
    val stockSymbol: String?,
    val stockPrice: Double?,
    val imageUrl: String?,
    val videoUrl: String?
)

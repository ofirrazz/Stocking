package com.stocksocial.model.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class CachedArticleEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val summary: String,
    val author: String,
    val content: String,
    val source: String,
    val imageUrl: String?,
    val publishedAt: String?,
    val url: String?,
    val localImagePath: String?
)

package com.stocksocial.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var isDeleted: Boolean = false // לניהול סנכרון אם תרצה
)

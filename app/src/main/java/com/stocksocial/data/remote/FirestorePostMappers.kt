package com.stocksocial.data.remote

import com.google.firebase.firestore.DocumentSnapshot
import com.stocksocial.model.Post
import com.stocksocial.model.cache.CachedPostEntity
import com.stocksocial.model.cache.toPost
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun DocumentSnapshot.toCachedPostEntity(localImagePath: String? = null): CachedPostEntity? {
    val docId = id
    val authorId = getString("authorId") ?: return null
    val authorUsername = getString("authorUsername") ?: "user"
    val content = getString("content") ?: ""
    val millis = getLong("createdAt") ?: 0L
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val createdAtLabel = sdf.format(Date(millis))
    val likes = (getLong("likesCount") ?: (get("likesCount") as? Number)?.toLong() ?: 0L).toInt()
    val comments = (getLong("commentsCount") ?: (get("commentsCount") as? Number)?.toLong() ?: 0L).toInt()
    val price = (get("stockPrice") as? Number)?.toDouble()
    return CachedPostEntity(
        id = docId,
        authorId = authorId,
        authorUsername = authorUsername,
        content = content,
        createdAt = createdAtLabel,
        createdAtMillis = millis,
        likesCount = likes,
        commentsCount = comments,
        stockSymbol = getString("stockSymbol"),
        stockPrice = price,
        imageUrl = getString("imageUrl"),
        localImagePath = localImagePath
    )
}

fun DocumentSnapshot.toPost(): Post? = toCachedPostEntity()?.toPost()

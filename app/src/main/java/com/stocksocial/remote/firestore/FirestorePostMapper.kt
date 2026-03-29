package com.stocksocial.remote.firestore

import com.stocksocial.model.Post
import com.stocksocial.model.User

fun FirestorePostDto.toDomainPost(): Post {
    return Post(
        id = id,
        author = User(
            id = userId,
            username = username,
            email = "$userId@stocksocial.remote"
        ),
        content = text,
        createdAt = timestamp.toString(),
        likesCount = likesCount,
        commentsCount = commentsCount,
        stockSymbol = stockSymbol,
        stockPrice = null,
        imageUrl = imageUrl,
        videoUrl = null
    )
}

fun Post.toFirestoreDto(): FirestorePostDto {
    return FirestorePostDto(
        id = id,
        userId = author.id,
        username = author.username,
        text = content,
        stockSymbol = stockSymbol,
        imageUrl = imageUrl,
        timestamp = createdAt.toLongOrNull() ?: System.currentTimeMillis(),
        likesCount = likesCount,
        commentsCount = commentsCount
    )
}

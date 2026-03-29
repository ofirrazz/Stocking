package com.stocksocial.remote.firestore

data class FirestorePostDto(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val stockSymbol: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)

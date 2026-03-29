package com.stocksocial.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.stocksocial.local.PostDao
import com.stocksocial.local.PostEntity
import com.stocksocial.model.Post
import com.stocksocial.model.User
import java.util.UUID

class LocalPostsRepository(
    private val postDao: PostDao
) {
    fun observePostsByUser(userId: String): LiveData<List<Post>> {
        return postDao.observePostsByUser(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun createPost(
        userId: String,
        username: String,
        content: String,
        imageUrl: String?
    ) {
        postDao.upsert(
            PostEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                username = username,
                content = content,
                createdAt = System.currentTimeMillis().toString(),
                likesCount = 0,
                commentsCount = 0,
                stockSymbol = null,
                stockPrice = null,
                imageUrl = imageUrl,
                videoUrl = null
            )
        )
    }

    suspend fun deletePost(postId: String) {
        postDao.deleteById(postId)
    }

    private fun PostEntity.toDomain(): Post {
        return Post(
            id = id,
            author = User(
                id = userId,
                username = username,
                email = "$userId@stocksocial.local"
            ),
            content = content,
            createdAt = createdAt,
            likesCount = likesCount,
            commentsCount = commentsCount,
            stockSymbol = stockSymbol,
            stockPrice = stockPrice,
            imageUrl = imageUrl,
            videoUrl = videoUrl
        )
    }
}

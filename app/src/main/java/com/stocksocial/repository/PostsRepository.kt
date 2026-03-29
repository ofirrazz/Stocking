package com.stocksocial.repository

import com.stocksocial.model.Post
import com.stocksocial.remote.firestore.FirestorePostDataSource
import com.stocksocial.remote.firestore.toDomainPost
import com.stocksocial.remote.firestore.toFirestoreDto

class PostsRepository(
    private val localPostsRepository: LocalPostsRepository,
    private val firestorePostDataSource: FirestorePostDataSource
) {

    suspend fun syncUserPostsFromRemote(userId: String): RepositoryResult<Unit> {
        return when (val remote = firestorePostDataSource.getPostsByUser(userId)) {
            is RepositoryResult.Success -> {
                localPostsRepository.replacePostsForUser(
                    userId = userId,
                    posts = remote.data.map { it.toDomainPost() }
                )
                RepositoryResult.Success(Unit)
            }
            is RepositoryResult.Error -> remote
        }
    }

    suspend fun pushPostToRemote(post: Post): RepositoryResult<Unit> {
        return firestorePostDataSource.upsertPost(post.toFirestoreDto())
    }

    suspend fun deletePostRemote(postId: String): RepositoryResult<Unit> {
        return firestorePostDataSource.deletePost(postId)
    }
}

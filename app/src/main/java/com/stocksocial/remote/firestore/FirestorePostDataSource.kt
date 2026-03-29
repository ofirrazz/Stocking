package com.stocksocial.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.utils.Constants
import kotlinx.coroutines.tasks.await

class FirestorePostDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun upsertPost(post: FirestorePostDto): RepositoryResult<Unit> {
        return runCatching {
            firestore.collection(Constants.POSTS_COLLECTION)
                .document(post.id)
                .set(post)
                .await()
        }.fold(
            onSuccess = { RepositoryResult.Success(Unit) },
            onFailure = { RepositoryResult.Error(it.message ?: "Failed to save post", it) }
        )
    }

    suspend fun deletePost(postId: String): RepositoryResult<Unit> {
        return runCatching {
            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .delete()
                .await()
        }.fold(
            onSuccess = { RepositoryResult.Success(Unit) },
            onFailure = { RepositoryResult.Error(it.message ?: "Failed to delete post", it) }
        )
    }

    suspend fun getPostsByUser(userId: String): RepositoryResult<List<FirestorePostDto>> {
        return runCatching {
            firestore.collection(Constants.POSTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(FirestorePostDto::class.java)?.copy(id = doc.id)
                }
        }.fold(
            onSuccess = { RepositoryResult.Success(it) },
            onFailure = { RepositoryResult.Error(it.message ?: "Failed loading user posts", it) }
        )
    }

    suspend fun getAllPosts(): RepositoryResult<List<FirestorePostDto>> {
        return runCatching {
            firestore.collection(Constants.POSTS_COLLECTION)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(FirestorePostDto::class.java)?.copy(id = doc.id)
                }
        }.fold(
            onSuccess = { RepositoryResult.Success(it) },
            onFailure = { RepositoryResult.Error(it.message ?: "Failed loading posts", it) }
        )
    }
}

package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stocksocial.data.local.PostDao
import com.stocksocial.data.remote.toCachedPostEntity
import com.stocksocial.model.Post
import com.stocksocial.model.User
import com.stocksocial.model.cache.toPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val postDao: PostDao
) {

    suspend fun getProfile(): RepositoryResult<User> = withContext(Dispatchers.IO) {
        val u = auth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        try {
            val doc = firestore.collection("users").document(u.uid).get().await()
            val username = doc.getString("username")
                ?: u.displayName
                ?: u.email?.substringBefore("@")
                ?: "user"
            RepositoryResult.Success(
                User(
                    id = u.uid,
                    username = username,
                    email = u.email.orEmpty(),
                    avatarUrl = doc.getString("photoUrl")?.takeIf { it.isNotBlank() },
                    bio = doc.getString("bio")
                )
            )
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Profile load failed", e)
        }
    }

    suspend fun getMyPosts(): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        val u = auth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        try {
            val snap = firestore.collection("posts")
                .whereEqualTo("authorId", u.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val entities = snap.documents.mapNotNull { doc ->
                val existing = postDao.getById(doc.id)
                doc.toCachedPostEntity(existing?.localImagePath)
            }
            postDao.upsertAll(entities)
            val posts = entities.map { it.toPost() }
            RepositoryResult.Success(posts)
        } catch (e: Exception) {
            val cached = postDao.getAll()
                .filter { it.authorId == u.uid }
                .map { it.toPost() }
            if (cached.isNotEmpty()) {
                RepositoryResult.Success(cached)
            } else {
                RepositoryResult.Error(e.message ?: "Failed to load your posts", e)
            }
        }
    }
}

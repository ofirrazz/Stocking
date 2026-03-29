package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.stocksocial.data.local.PostDao
import com.stocksocial.data.remote.toCachedPostEntity
import com.stocksocial.model.Post
import com.stocksocial.model.User
import com.stocksocial.model.cache.toPost
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ProfileRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val postDao: PostDao,
    private val storage: FirebaseStorage
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

    suspend fun updateProfile(
        newName: String?,
        newImageUri: Uri?
    ): RepositoryResult<User> = withContext(Dispatchers.IO) {
        val current = auth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        try {
            var photoUrl: String? = null
            if (newImageUri != null) {
                val ref = storage.reference.child("profile_images/${current.uid}/${UUID.randomUUID()}.jpg")
                ref.putFile(newImageUri).await()
                photoUrl = ref.downloadUrl.await().toString()
            }

            val updates = mutableMapOf<String, Any>()
            val name = newName?.trim().orEmpty()
            if (name.isNotBlank()) updates["username"] = name
            if (!photoUrl.isNullOrBlank()) updates["photoUrl"] = photoUrl
            if (updates.isNotEmpty()) {
                firestore.collection("users").document(current.uid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            }

            if (name.isNotBlank() || !photoUrl.isNullOrBlank()) {
                val builder = UserProfileChangeRequest.Builder()
                if (name.isNotBlank()) builder.setDisplayName(name)
                if (!photoUrl.isNullOrBlank()) builder.setPhotoUri(Uri.parse(photoUrl))
                current.updateProfile(builder.build()).await()
            }

            getProfile()
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to update profile", e)
        }
    }
}

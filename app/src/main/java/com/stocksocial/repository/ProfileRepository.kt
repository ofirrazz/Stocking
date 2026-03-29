package com.stocksocial.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.stocksocial.data.remote.toPost
import com.stocksocial.model.Post
import com.stocksocial.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ProfileRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
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
            val posts = snap.documents.mapNotNull { it.toPost() }
            RepositoryResult.Success(posts)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load your posts", e)
        }
    }

    suspend fun updateProfile(displayName: String, photoUri: Uri?): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            val u = auth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
            val name = displayName.trim()
            if (name.isEmpty()) {
                return@withContext RepositoryResult.Error("Name cannot be empty")
            }
            try {
                var photoUrl: String? = null
                if (photoUri != null) {
                    val ref = storage.reference.child("profile_images/${u.uid}/${UUID.randomUUID()}.jpg")
                    ref.putFile(photoUri).await()
                    photoUrl = ref.downloadUrl.await().toString()
                }
                u.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(name).build()
                ).await()
                val updates = hashMapOf<String, Any>("username" to name)
                if (photoUrl != null) {
                    updates["photoUrl"] = photoUrl
                }
                firestore.collection("users").document(u.uid).update(updates).await()
                RepositoryResult.Success(Unit)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Profile update failed", e)
            }
        }
}

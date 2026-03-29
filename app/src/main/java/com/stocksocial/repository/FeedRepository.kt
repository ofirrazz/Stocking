package com.stocksocial.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.stocksocial.data.local.PostDao
import com.stocksocial.data.remote.toCachedPostEntity
import com.stocksocial.model.Post
import com.stocksocial.model.cache.toPost
import com.stocksocial.utils.ImageCacheDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class FeedRepository(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val postDao: PostDao,
    private val appContext: Context
) {

    suspend fun getFeedPosts(): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val baseEntities = snapshot.documents.mapNotNull { doc ->
                val existing = postDao.getById(doc.id)
                doc.toCachedPostEntity(existing?.localImagePath)
            }
            postDao.upsertAll(baseEntities)

            val posts = baseEntities.map { entity ->
                var local = entity.localImagePath
                val url = entity.imageUrl
                if (local == null && !url.isNullOrBlank()) {
                    local = ImageCacheDownloader.download(
                        appContext,
                        url,
                        "post_images",
                        entity.id.replace("/", "_") + ".img"
                    )
                    if (local != null) {
                        val updated = entity.copy(localImagePath = local)
                        postDao.upsert(updated)
                        updated.toPost()
                    } else {
                        entity.toPost()
                    }
                } else {
                    entity.toPost()
                }
            }
            RepositoryResult.Success(posts)
        } catch (e: Exception) {
            val cached = postDao.getAll().map { it.toPost() }
            if (cached.isNotEmpty()) {
                RepositoryResult.Success(cached)
            } else {
                RepositoryResult.Error(e.message ?: "Failed to load feed", e)
            }
        }
    }

    suspend fun publishPost(content: String, imageUri: Uri?): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            val user = auth.currentUser
                ?: return@withContext RepositoryResult.Error("You must be signed in to post")
            if (content.isBlank() && imageUri == null) {
                return@withContext RepositoryResult.Error("Post cannot be empty")
            }
            try {
                var downloadUrl: String? = null
                if (imageUri != null) {
                    val ref = storage.reference.child("posts_images/${user.uid}/${UUID.randomUUID()}.jpg")
                    ref.putFile(imageUri).await()
                    downloadUrl = ref.downloadUrl.await().toString()
                }
                val doc = firestore.collection("posts").document()
                val millis = System.currentTimeMillis()
                val data = hashMapOf<String, Any?>(
                    "id" to doc.id,
                    "authorId" to user.uid,
                    "authorUsername" to (user.displayName ?: user.email?.substringBefore("@") ?: "user"),
                    "content" to content.trim(),
                    "imageUrl" to downloadUrl,
                    "createdAt" to millis,
                    "likesCount" to 0,
                    "commentsCount" to 0,
                    "stockSymbol" to null,
                    "stockPrice" to null
                )
                doc.set(data).await()
                RepositoryResult.Success(Unit)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Publish failed", e)
            }
        }
}

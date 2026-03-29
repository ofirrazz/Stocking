package com.stocksocial.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
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

    suspend fun getPostForEdit(postId: String): RepositoryResult<Post> =
        withContext(Dispatchers.IO) {
            val user = auth.currentUser
                ?: return@withContext RepositoryResult.Error("You must be signed in")
            try {
                val doc = firestore.collection("posts").document(postId).get().await()
                if (!doc.exists()) {
                    return@withContext RepositoryResult.Error("Post not found")
                }
                val authorId = doc.getString("authorId")
                    ?: return@withContext RepositoryResult.Error("Invalid post")
                if (authorId != user.uid) {
                    return@withContext RepositoryResult.Error("You can only edit your own posts")
                }
                val entity = doc.toCachedPostEntity(postDao.getById(postId)?.localImagePath)
                    ?: return@withContext RepositoryResult.Error("Invalid post data")
                RepositoryResult.Success(entity.toPost())
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Failed to load post", e)
            }
        }

    suspend fun updatePost(
        postId: String,
        content: String,
        newImageUri: Uri?,
        removeImage: Boolean
    ): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
            ?: return@withContext RepositoryResult.Error("You must be signed in")
        if (content.isBlank() && newImageUri == null && !removeImage) {
            return@withContext RepositoryResult.Error("Post cannot be empty")
        }
        try {
            val docRef = firestore.collection("posts").document(postId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return@withContext RepositoryResult.Error("Post not found")
            }
            if (snap.getString("authorId") != user.uid) {
                return@withContext RepositoryResult.Error("You can only edit your own posts")
            }
            var imageUrl: String? = snap.getString("imageUrl")
            val oldUrl = imageUrl
            if (removeImage) {
                tryDeleteStorageFile(oldUrl)
                imageUrl = null
            }
            if (newImageUri != null) {
                tryDeleteStorageFile(oldUrl)
                val ref = storage.reference.child("posts_images/${user.uid}/${UUID.randomUUID()}.jpg")
                ref.putFile(newImageUri).await()
                imageUrl = ref.downloadUrl.await().toString()
            }
            if (content.isBlank() && imageUrl == null) {
                return@withContext RepositoryResult.Error("Post must have text or an image")
            }
            val display = user.displayName ?: user.email?.substringBefore("@") ?: "user"
            val updates = hashMapOf<String, Any?>(
                "content" to content.trim(),
                "imageUrl" to imageUrl,
                "authorUsername" to display
            )
            docRef.update(updates).await()
            syncPostInCache(docRef.get().await())
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Update failed", e)
        }
    }

    suspend fun deletePost(postId: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val user = auth.currentUser
            ?: return@withContext RepositoryResult.Error("You must be signed in")
        try {
            val docRef = firestore.collection("posts").document(postId)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                return@withContext RepositoryResult.Error("Post not found")
            }
            if (snap.getString("authorId") != user.uid) {
                return@withContext RepositoryResult.Error("You can only delete your own posts")
            }
            tryDeleteStorageFile(snap.getString("imageUrl"))
            docRef.delete().await()
            postDao.deleteById(postId)
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Delete failed", e)
        }
    }

    private suspend fun syncPostInCache(doc: DocumentSnapshot) {
        val entity = doc.toCachedPostEntity(null) ?: return
        val url = entity.imageUrl
        val local = if (!url.isNullOrBlank()) {
            ImageCacheDownloader.download(
                appContext,
                url,
                "post_images",
                entity.id.replace("/", "_") + ".img"
            )
        } else null
        postDao.upsert(entity.copy(localImagePath = local))
    }

    private suspend fun tryDeleteStorageFile(url: String?) {
        if (url.isNullOrBlank()) return
        try {
            storage.getReferenceFromUrl(url).delete().await()
        } catch (_: Exception) {
        }
    }
}

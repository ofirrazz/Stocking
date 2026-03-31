package com.stocksocial.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.stocksocial.data.local.PostDao
import com.stocksocial.data.remote.toCachedPostEntity
import com.stocksocial.model.Post
import com.stocksocial.model.PostComment
import com.stocksocial.model.cache.CachedPostEntity
import com.stocksocial.model.cache.toPost
import com.stocksocial.utils.ImageCacheDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FeedRepository(
    private val firestore: FirebaseFirestore?,
    private val storage: FirebaseStorage?,
    private val auth: FirebaseAuth?,
    private val postDao: PostDao,
    private val appContext: Context,
    private val watchlistRepository: WatchlistRepository
) {

    suspend fun getFeedPosts(): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        val firebaseFirestore = firestore
            ?: return@withContext fallbackToCache("Firebase is not configured for feed.")
        try {
            val remoteEntities = fetchRemoteEntities(firebaseFirestore)
            val withQuotes = mergeLatestQuotePrices(remoteEntities)
            cacheEntities(withQuotes)
            val hydrated = hydrateEntitiesWithImages(withQuotes)
            RepositoryResult.Success(hydrated.map { it.toPost() })
        } catch (e: Exception) {
            fallbackToCache(e.message ?: "Failed to load feed", e)
        }
    }

    suspend fun publishPost(content: String, imageUri: Uri?): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            val firebaseAuth = auth
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val firebaseFirestore = firestore
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val firebaseStorage = storage
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val user = firebaseAuth.currentUser
                ?: return@withContext RepositoryResult.Error("You must be signed in to post")
            if (content.isBlank() && imageUri == null) {
                return@withContext RepositoryResult.Error("Post cannot be empty")
            }
            try {
                var imageUrl: String? = null
                var videoUrl: String? = null
                if (imageUri != null) {
                    val mime = appContext.contentResolver.getType(imageUri).orEmpty()
                    val isVideo = mime.startsWith("video/")
                    val ext = mediaExtensionForMime(mime, isVideo)
                    val folder = if (isVideo) "posts_videos" else "posts_images"
                    val ref = firebaseStorage.reference
                        .child("$folder/${user.uid}/${UUID.randomUUID()}.$ext")
                    ref.putFile(imageUri).await()
                    val url = ref.downloadUrl.await().toString()
                    if (isVideo) videoUrl = url else imageUrl = url
                }
                val doc = firebaseFirestore.collection("posts").document()
                doc.set(
                    buildPostPayload(
                        doc.id,
                        user.uid,
                        user.displayName,
                        user.email,
                        content,
                        imageUrl,
                        videoUrl
                    )
                ).await()
                RepositoryResult.Success(Unit)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Publish failed", e)
            }
        }

    suspend fun getPostById(postId: String): RepositoryResult<Post> = withContext(Dispatchers.IO) {
        val firebaseFirestore = firestore
            ?: return@withContext postDao.getById(postId)?.toPost()?.let { RepositoryResult.Success(it) }
            ?: RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val cached = postDao.getById(postId)?.toPost()
        try {
            val doc = firebaseFirestore.collection("posts").document(postId).get().await()
            val entity = doc.toCachedPostEntity(
                postDao.getById(postId)?.localImagePath,
                auth?.currentUser?.uid
            )
                ?: return@withContext cached?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.Error("Post not found")

            val hydrated = hydrateEntityWithLocalImage(entity)
            postDao.upsert(hydrated)
            RepositoryResult.Success(hydrated.toPost())
        } catch (e: Exception) {
            cached?.let { RepositoryResult.Success(it) }
                ?: RepositoryResult.Error(e.message ?: "Failed to load post", e)
        }
    }

    suspend fun updatePost(postId: String, content: String, imageUri: Uri?): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            val firebaseAuth = auth
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val firebaseFirestore = firestore
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val firebaseStorage = storage
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val user = firebaseAuth.currentUser
                ?: return@withContext RepositoryResult.Error("You must be signed in")
            if (content.isBlank() && imageUri == null) {
                return@withContext RepositoryResult.Error("Post cannot be empty")
            }
            try {
                val postRef = firebaseFirestore.collection("posts").document(postId)
                val snapshot = postRef.get().await()
                val authorId = snapshot.getString("authorId")
                if (authorId != user.uid) {
                    return@withContext RepositoryResult.Error("You can edit only your own posts")
                }

                var imageUrl = snapshot.getString("imageUrl")
                if (imageUri != null) {
                    val ref = firebaseStorage.reference.child("posts_images/${user.uid}/${UUID.randomUUID()}.jpg")
                    ref.putFile(imageUri).await()
                    imageUrl = ref.downloadUrl.await().toString()
                }

                postRef.update(
                    mapOf(
                        "content" to content.trim(),
                        "imageUrl" to imageUrl
                    )
                ).await()

                getPostById(postId)
                RepositoryResult.Success(Unit)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Failed to update post", e)
            }
        }

    suspend fun deletePost(postId: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val user = firebaseAuth.currentUser
            ?: return@withContext RepositoryResult.Error("You must be signed in")
        try {
            val postRef = firebaseFirestore.collection("posts").document(postId)
            val snapshot = postRef.get().await()
            val authorId = snapshot.getString("authorId")
            if (authorId != user.uid) {
                return@withContext RepositoryResult.Error("You can delete only your own posts")
            }
            postRef.delete().await()
            postDao.deleteById(postId)
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to delete post", e)
        }
    }

    private suspend fun fetchRemoteEntities(firebaseFirestore: FirebaseFirestore): List<CachedPostEntity> {
        val snapshot = firebaseFirestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        val uid = auth?.currentUser?.uid
        return snapshot.documents.mapNotNull { doc ->
            val existing = postDao.getById(doc.id)
            doc.toCachedPostEntity(existing?.localImagePath, uid)
        }
    }

    private suspend fun cacheEntities(entities: List<CachedPostEntity>) {
        if (entities.isNotEmpty()) {
            postDao.upsertAll(entities)
        }
    }

    private suspend fun hydrateEntitiesWithImages(entities: List<CachedPostEntity>): List<CachedPostEntity> {
        return entities.map { entity -> hydrateEntityWithLocalImage(entity) }
    }

    private suspend fun hydrateEntityWithLocalImage(entity: CachedPostEntity): CachedPostEntity {
        if (!entity.localImagePath.isNullOrBlank()) return entity
        val remoteUrl = entity.imageUrl ?: return entity
        val localPath = ImageCacheDownloader.download(
            appContext,
            remoteUrl,
            "post_images",
            entity.id.replace("/", "_") + ".img"
        ) ?: return entity

        val updated = entity.copy(localImagePath = localPath)
        postDao.upsert(updated)
        return updated
    }

    suspend fun getCachedPosts(): List<Post> {
        return postDao.getAll().map { it.toPost() }
    }

    suspend fun refreshQuotesForPosts(posts: List<Post>): List<Post> = withContext(Dispatchers.IO) {
        val symbols = posts.mapNotNull { it.stockSymbol?.trim()?.uppercase() }.distinct()
        if (symbols.isEmpty()) return@withContext posts
        val prices = watchlistRepository.getLatestPrices(symbols)
        if (prices.isEmpty()) return@withContext posts
        posts.map { p ->
            val sym = p.stockSymbol?.uppercase() ?: return@map p
            val price = prices[sym] ?: return@map p
            p.copy(stockPrice = price)
        }
    }

    private suspend fun mergeLatestQuotePrices(entities: List<CachedPostEntity>): List<CachedPostEntity> {
        val symbols = entities.mapNotNull { it.stockSymbol?.trim()?.uppercase() }.distinct()
        if (symbols.isEmpty()) return entities
        val prices = watchlistRepository.getLatestPrices(symbols)
        if (prices.isEmpty()) return entities
        return entities.map { e ->
            val sym = e.stockSymbol?.uppercase() ?: return@map e
            val price = prices[sym] ?: return@map e
            e.copy(stockPrice = price)
        }
    }

    private suspend fun fallbackToCache(
        errorMessage: String,
        throwable: Throwable? = null
    ): RepositoryResult<List<Post>> {
        val cached = getCachedPosts()
        return if (cached.isNotEmpty()) {
            RepositoryResult.Success(cached)
        } else {
            RepositoryResult.Error(errorMessage, throwable)
        }
    }

    private fun buildPostPayload(
        postId: String,
        authorId: String,
        displayName: String?,
        email: String?,
        content: String,
        imageUrl: String?,
        videoUrl: String?
    ): HashMap<String, Any?> {
        val authorUsername = displayName ?: email?.substringBefore("@") ?: "user"
        val normalizedContent = content.trim()
        val extractedSymbol = extractTickerSymbol(normalizedContent)
        return hashMapOf(
            "id" to postId,
            "authorId" to authorId,
            "authorUsername" to authorUsername,
            "content" to normalizedContent,
            "imageUrl" to imageUrl,
            "videoUrl" to videoUrl,
            "createdAt" to System.currentTimeMillis(),
            "likesCount" to 0,
            "likedUserIds" to emptyList<String>(),
            "commentsCount" to 0,
            "stockSymbol" to extractedSymbol,
            "stockPrice" to null
        )
    }

    private fun mediaExtensionForMime(mime: String, isVideo: Boolean): String {
        val m = mime.lowercase()
        return when {
            isVideo -> when {
                m.contains("webm") -> "webm"
                m.contains("3gp") -> "3gp"
                m.contains("quicktime") || m.contains("mov") -> "mov"
                else -> "mp4"
            }
            else -> when {
                m.contains("png") -> "png"
                m.contains("webp") -> "webp"
                m.contains("gif") -> "gif"
                else -> "jpg"
            }
        }
    }

    private fun extractTickerSymbol(content: String): String? {
        val regex = Regex("""\$(\p{Alpha}{1,6})\b""")
        return regex.find(content)?.groupValues?.getOrNull(1)?.uppercase()
    }

    suspend fun getComments(postId: String): RepositoryResult<List<PostComment>> = withContext(Dispatchers.IO) {
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        if (postId.isBlank()) return@withContext RepositoryResult.Error("Invalid post")
        try {
            val snap = firebaseFirestore.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(100)
                .get()
                .await()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val list = snap.documents.map { doc ->
                val millis = doc.getLong("createdAt") ?: 0L
                PostComment(
                    id = doc.id,
                    authorUsername = doc.getString("authorUsername") ?: "user",
                    content = doc.getString("content") ?: "",
                    createdAt = sdf.format(Date(millis))
                )
            }
            RepositoryResult.Success(list)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load comments", e)
        }
    }

    suspend fun addComment(postId: String, text: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val user = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Error("You must be signed in")
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return@withContext RepositoryResult.Error("Comment cannot be empty")
        if (postId.isBlank()) return@withContext RepositoryResult.Error("Invalid post")
        try {
            val postRef = firebaseFirestore.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document()
            val username = user.displayName ?: user.email?.substringBefore("@") ?: "user"
            val batch = firebaseFirestore.batch()
            batch.set(
                commentRef,
                mapOf(
                    "authorId" to user.uid,
                    "authorUsername" to username,
                    "content" to trimmed,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            batch.update(postRef, "commentsCount", FieldValue.increment(1))
            batch.commit().await()

            val local = postDao.getById(postId)
            if (local != null) {
                postDao.upsert(local.copy(commentsCount = local.commentsCount + 1))
            }
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to post comment", e)
        }
    }

    companion object {
        private const val FIREBASE_NOT_CONFIGURED_MESSAGE =
            "Firebase is not configured on this build. Add app/google-services.json to enable feed actions."
    }
}

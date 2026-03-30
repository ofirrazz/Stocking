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
import com.stocksocial.model.cache.CachedPostEntity
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
    private val appContext: Context,
    private val watchlistRepository: WatchlistRepository
) {

    suspend fun getFeedPosts(): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val remoteEntities = fetchRemoteEntities()
            val withQuotes = mergeLatestQuotePrices(remoteEntities)
            cacheEntities(withQuotes)
            val hydrated = hydrateEntitiesWithImages(withQuotes)
            RepositoryResult.Success(hydrated.map { it.toPost() })
        } catch (e: Exception) {
            val cached = getCachedPosts()
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
                doc.set(buildPostPayload(doc.id, user.uid, user.displayName, user.email, content, downloadUrl))
                    .await()
                RepositoryResult.Success(Unit)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Publish failed", e)
            }
        }

    suspend fun getPostById(postId: String): RepositoryResult<Post> = withContext(Dispatchers.IO) {
        val cached = postDao.getById(postId)?.toPost()
        try {
            val doc = firestore.collection("posts").document(postId).get().await()
            val entity = doc.toCachedPostEntity(postDao.getById(postId)?.localImagePath)
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
            val user = auth.currentUser
                ?: return@withContext RepositoryResult.Error("You must be signed in")
            if (content.isBlank() && imageUri == null) {
                return@withContext RepositoryResult.Error("Post cannot be empty")
            }
            try {
                val postRef = firestore.collection("posts").document(postId)
                val snapshot = postRef.get().await()
                val authorId = snapshot.getString("authorId")
                if (authorId != user.uid) {
                    return@withContext RepositoryResult.Error("You can edit only your own posts")
                }

                var imageUrl = snapshot.getString("imageUrl")
                if (imageUri != null) {
                    val ref = storage.reference.child("posts_images/${user.uid}/${UUID.randomUUID()}.jpg")
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
        val user = auth.currentUser
            ?: return@withContext RepositoryResult.Error("You must be signed in")
        try {
            val postRef = firestore.collection("posts").document(postId)
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

    private suspend fun fetchRemoteEntities(): List<CachedPostEntity> {
        val snapshot = firestore.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val existing = postDao.getById(doc.id)
            doc.toCachedPostEntity(existing?.localImagePath)
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

    private fun buildPostPayload(
        postId: String,
        authorId: String,
        displayName: String?,
        email: String?,
        content: String,
        imageUrl: String?
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
            "createdAt" to System.currentTimeMillis(),
            "likesCount" to 0,
            "commentsCount" to 0,
            "stockSymbol" to extractedSymbol,
            "stockPrice" to null
        )
    }

    private fun extractTickerSymbol(content: String): String? {
        val regex = Regex("""\$(\p{Alpha}{1,6})\b""")
        return regex.find(content)?.groupValues?.getOrNull(1)?.uppercase()
    }
}

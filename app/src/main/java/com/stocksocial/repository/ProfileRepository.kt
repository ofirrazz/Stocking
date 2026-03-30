package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.stocksocial.data.local.PostDao
import com.stocksocial.data.remote.toCachedPostEntity
import com.stocksocial.data.remote.toPost
import com.stocksocial.model.Post
import com.stocksocial.model.PublicUserProfile
import com.stocksocial.model.User
import com.stocksocial.model.UserSuggestion
import com.stocksocial.model.cache.toPost
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class ProfileRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val postDao: PostDao,
    private val storage: FirebaseStorage
) {

    suspend fun searchUsersByPrefix(query: String): RepositoryResult<List<UserSuggestion>> =
        withContext(Dispatchers.IO) {
            val currentUid = auth.currentUser?.uid
                ?: return@withContext RepositoryResult.Error("Not signed in")
            val q = query.trim().lowercase()
            if (q.length < 2) return@withContext RepositoryResult.Success(emptyList())
            try {
                val snapshot = try {
                    firestore.collection("users")
                        .orderBy("usernameLower")
                        .startAt(q)
                        .endAt(q + "\uf8ff")
                        .limit(8)
                        .get()
                        .await()
                } catch (_: Exception) {
                    firestore.collection("users")
                        .orderBy("username")
                        .startAt(q)
                        .endAt(q + "\uf8ff")
                        .limit(8)
                        .get()
                        .await()
                }

                val items = snapshot.documents.mapNotNull { doc ->
                    if (doc.id == currentUid) return@mapNotNull null
                    val username = doc.getString("username")?.trim().orEmpty()
                    if (username.isBlank()) return@mapNotNull null
                    UserSuggestion(
                        id = doc.id,
                        username = username,
                        avatarUrl = doc.getString("photoUrl")
                    )
                }
                RepositoryResult.Success(items)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Search failed", e)
            }
        }

    suspend fun getProfile(): RepositoryResult<User> = withContext(Dispatchers.IO) {
        val u = auth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        try {
            val userRef = firestore.collection("users").document(u.uid)
            val loads = coroutineScope {
                val docD = async { userRef.get().await() }
                val folD = async { userRef.collection("followers").get().await() }
                val ingD = async { userRef.collection("following").get().await() }
                val postsD = async {
                    firestore.collection("posts")
                        .whereEqualTo("authorId", u.uid)
                        .limit(500)
                        .get()
                        .await()
                }
                TupleProfileLoads(
                    doc = docD.await(),
                    followers = folD.await().size(),
                    following = ingD.await().size(),
                    postsSnap = postsD.await()
                )
            }
            val doc = loads.doc
            val followersN = loads.followers
            val followingN = loads.following
            val postsSnap = loads.postsSnap
            val username = doc.getString("username")
                ?: u.displayName
                ?: u.email?.substringBefore("@")
                ?: "user"
            val displayName = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: username
            val postsCount = postsSnap.size()
            val totalLikes = postsSnap.documents.sumOf { (it.getLong("likesCount") ?: 0L).toInt() }
            RepositoryResult.Success(
                User(
                    id = u.uid,
                    username = username,
                    email = u.email.orEmpty(),
                    avatarUrl = doc.getString("photoUrl")?.takeIf { it.isNotBlank() },
                    bio = doc.getString("bio"),
                    displayName = displayName,
                    postsCount = postsCount,
                    followersCount = followersN,
                    followingCount = followingN,
                    totalLikesReceived = totalLikes
                )
            )
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Profile load failed", e)
        }
    }

    private data class TupleProfileLoads(
        val doc: com.google.firebase.firestore.DocumentSnapshot,
        val followers: Int,
        val following: Int,
        val postsSnap: com.google.firebase.firestore.QuerySnapshot
    )

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
            if (name.isNotBlank()) {
                updates["username"] = name
                updates["usernameLower"] = name.lowercase()
                updates["displayName"] = name
            }
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

    suspend fun followUserByUsername(username: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val current = auth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        val normalized = username.trim()
        if (normalized.isBlank()) {
            return@withContext RepositoryResult.Error("Enter a username")
        }
        try {
            val targetSnapshot = firestore.collection("users")
                .whereEqualTo("username", normalized)
                .limit(1)
                .get()
                .await()
            val targetDoc = targetSnapshot.documents.firstOrNull()
                ?: return@withContext RepositoryResult.Error("User not found")
            val targetId = targetDoc.id
            if (targetId == current.uid) {
                return@withContext RepositoryResult.Error("You cannot follow yourself")
            }

            val now = System.currentTimeMillis()
            firestore.collection("users")
                .document(current.uid)
                .collection("following")
                .document(targetId)
                .set(
                    mapOf(
                        "userId" to targetId,
                        "username" to (targetDoc.getString("username") ?: normalized),
                        "followedAt" to now
                    )
                )
                .await()

            firestore.collection("users")
                .document(targetId)
                .collection("followers")
                .document(current.uid)
                .set(
                    mapOf(
                        "userId" to current.uid,
                        "username" to (current.displayName ?: current.email?.substringBefore("@") ?: "user"),
                        "followedAt" to now
                    )
                )
                .await()

            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to follow user", e)
        }
    }

    suspend fun likePost(postId: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val current = auth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        try {
            firestore.collection("posts")
                .document(postId)
                .update(
                    mapOf(
                        "likesCount" to FieldValue.increment(1),
                        "lastLikedBy" to current.uid
                    )
                )
                .await()

            val local = postDao.getById(postId)
            if (local != null) {
                postDao.upsert(local.copy(likesCount = local.likesCount + 1))
            }
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to like post", e)
        }
    }

    suspend fun getPublicProfileByUsername(username: String): RepositoryResult<PublicUserProfile> =
        withContext(Dispatchers.IO) {
            val normalized = username.trim()
            if (normalized.isBlank()) return@withContext RepositoryResult.Error("Invalid username")
            try {
                var snap = firestore.collection("users")
                    .whereEqualTo("usernameLower", normalized.lowercase())
                    .limit(1)
                    .get()
                    .await()
                if (snap.isEmpty) {
                    snap = firestore.collection("users")
                        .whereEqualTo("username", normalized)
                        .limit(1)
                        .get()
                        .await()
                }
                val doc = snap.documents.firstOrNull()
                    ?: return@withContext RepositoryResult.Error("User not found")
                val uid = doc.id
                val uname = doc.getString("username").orEmpty().ifBlank { normalized }
                val displayName = doc.getString("displayName")?.takeIf { it.isNotBlank() } ?: uname
                val stats = coroutineScope {
                    val followersD = async {
                        firestore.collection("users").document(uid).collection("followers").get().await()
                    }
                    val followingD = async {
                        firestore.collection("users").document(uid).collection("following").get().await()
                    }
                    val postsD = async {
                        firestore.collection("posts")
                            .whereEqualTo("authorId", uid)
                            .limit(500)
                            .get()
                            .await()
                    }
                    Triple(followersD.await(), followingD.await(), postsD.await())
                }
                val (followers, following, postsSnap) = stats
                val postsCount = postsSnap.size()
                val totalLikes = postsSnap.documents.sumOf { (it.getLong("likesCount") ?: 0L).toInt() }
                val createdAt = doc.getLong("createdAt")
                RepositoryResult.Success(
                    PublicUserProfile(
                        userId = uid,
                        username = uname,
                        displayName = displayName,
                        bio = doc.getString("bio").orEmpty(),
                        avatarUrl = doc.getString("photoUrl")?.takeIf { it.isNotBlank() },
                        bannerUrl = doc.getString("bannerUrl")?.takeIf { it.isNotBlank() },
                        location = doc.getString("location")?.takeIf { it.isNotBlank() },
                        website = doc.getString("website")?.takeIf { it.isNotBlank() },
                        joinedTimestamp = createdAt,
                        followersCount = followers.documents.size,
                        followingCount = following.documents.size,
                        postsCount = postsCount,
                        totalLikesReceived = totalLikes
                    )
                )
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Failed to load profile", e)
            }
        }

    suspend fun getPostsByUserId(userId: String): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext RepositoryResult.Error("Invalid user")
        try {
            val snap = firestore.collection("posts")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            val posts = snap.documents.mapNotNull { it.toPost() }
            RepositoryResult.Success(posts)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load posts", e)
        }
    }

    suspend fun isSymbolFavorite(symbol: String): Boolean = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        val sym = symbol.trim().uppercase()
        if (sym.isBlank()) return@withContext false
        try {
            firestore.collection("users")
                .document(uid)
                .collection("favoriteSymbols")
                .document(sym)
                .get()
                .await()
                .exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun setSymbolFavorite(symbol: String, favorite: Boolean): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@withContext RepositoryResult.Error("Not signed in")
            val sym = symbol.trim().uppercase()
            if (sym.isBlank()) return@withContext RepositoryResult.Error("Invalid symbol")
            try {
                val ref = firestore.collection("users")
                    .document(uid)
                    .collection("favoriteSymbols")
                    .document(sym)
                if (favorite) {
                    ref.set(
                        mapOf(
                            "symbol" to sym,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
                } else {
                    ref.delete().await()
                }
                RepositoryResult.Success(Unit)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Failed to update favorite", e)
            }
        }
}

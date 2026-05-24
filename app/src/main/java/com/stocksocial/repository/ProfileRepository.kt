package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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
import java.util.concurrent.atomic.AtomicBoolean

class ProfileRepository(
    private val firestore: FirebaseFirestore?,
    private val auth: FirebaseAuth?,
    private val postDao: PostDao,
    private val storage: FirebaseStorage?
) {

    suspend fun searchUsersByPrefix(query: String): RepositoryResult<List<UserSuggestion>> =
        withContext(Dispatchers.IO) {
            val firebaseAuth = auth
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val firebaseFirestore = firestore
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val currentUid = firebaseAuth.currentUser?.uid
                ?: return@withContext RepositoryResult.Error("Not signed in")
            val q = query.trim().lowercase()
            if (q.length < 2) return@withContext RepositoryResult.Success(emptyList())
            try {
                val snapshot = try {
                    firebaseFirestore.collection("users")
                        .orderBy("usernameLower")
                        .startAt(q)
                        .endAt(q + "\uf8ff")
                        .limit(8)
                        .get()
                        .await()
                } catch (_: Exception) {
                    firebaseFirestore.collection("users")
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
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val u = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        try {
            val userRef = firebaseFirestore.collection("users").document(u.uid)
            // Each sub-query catches its own error and returns a sensible default so that
            // a single broken read (e.g. a temporarily missing follower-subcollection rule
            // or a network hiccup on one parallel call) doesn't blank the whole profile.
            val loads = coroutineScope {
                val docD = async {
                    runCatching {
                        UserFirestoreHelper.ensureUserProfileDocument(firebaseFirestore, u)
                    }.getOrElse {
                        runCatching { userRef.get().await() }.getOrNull()
                    }
                }
                val folD = async {
                    runCatching {
                        userRef.collection("followers").get().await().size()
                    }.getOrDefault(0)
                }
                val ingD = async {
                    runCatching {
                        userRef.collection("following").get().await().size()
                    }.getOrDefault(0)
                }
                val postsD = async {
                    runCatching {
                        firebaseFirestore.collection("posts")
                            .whereEqualTo("authorId", u.uid)
                            .limit(500)
                            .get()
                            .await()
                    }.getOrNull()
                }
                TupleProfileLoadsLenient(
                    doc = docD.await(),
                    followers = folD.await(),
                    following = ingD.await(),
                    postsSnap = postsD.await()
                )
            }
            val doc = loads.doc
            val followersN = loads.followers
            val followingN = loads.following
            val postsSnap = loads.postsSnap
            val username = doc?.getString("username")
                ?: u.displayName
                ?: u.email?.substringBefore("@")
                ?: "user"
            val displayName = doc?.getString("displayName")?.takeIf { it.isNotBlank() } ?: username
            // If posts query failed, fall back to the local Room cache so that the user
            // still sees a non-zero post count for posts they published earlier.
            val postsCount: Int
            val totalLikes: Int
            if (postsSnap != null) {
                postsCount = postsSnap.size()
                totalLikes = postsSnap.documents.sumOf { (it.getLong("likesCount") ?: 0L).toInt() }
            } else {
                val cached = postDao.getAll().filter { it.authorId == u.uid }
                postsCount = cached.size
                totalLikes = cached.sumOf { it.likesCount }
            }
            RepositoryResult.Success(
                User(
                    id = u.uid,
                    username = username,
                    email = u.email.orEmpty(),
                    avatarUrl = doc?.getString("photoUrl")?.takeIf { it.isNotBlank() },
                    bio = doc?.getString("bio"),
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

    private data class TupleProfileLoadsLenient(
        val doc: com.google.firebase.firestore.DocumentSnapshot?,
        val followers: Int,
        val following: Int,
        val postsSnap: com.google.firebase.firestore.QuerySnapshot?
    )

    suspend fun getMyPosts(): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val u = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        try {
            val docs = try {
                firebaseFirestore.collection("posts")
                    .whereEqualTo("authorId", u.uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()
                    .documents
            } catch (e: FirebaseFirestoreException) {
                if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    firebaseFirestore.collection("posts")
                        .whereEqualTo("authorId", u.uid)
                        .limit(50)
                        .get()
                        .await()
                        .documents
                        .sortedByDescending { it.getLong("createdAt") ?: 0L }
                } else throw e
            }
            val entities = docs.mapNotNull { doc ->
                val existing = postDao.getById(doc.id)
                doc.toCachedPostEntity(existing?.localImagePath, u.uid)
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
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseStorage = storage
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val current = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        try {
            var photoUrl: String? = null
            if (newImageUri != null) {
                val ref = firebaseStorage.reference.child("profile_images/${current.uid}/${UUID.randomUUID()}.jpg")
                ref.putFile(newImageUri).await()
                photoUrl = ref.downloadUrl.await().toString()
            }

            val updates = mutableMapOf<String, Any>()
            val name = newName?.trim().orEmpty()
            if (name.isNotBlank()) {
                // We treat the edit-profile field as the *display* name only. The username
                // (`@handle`) is immutable post-registration so that follow-by-username and
                // shared links keep working.
                updates["displayName"] = name
            }
            if (!photoUrl.isNullOrBlank()) updates["photoUrl"] = photoUrl
            if (updates.isNotEmpty()) {
                firebaseFirestore.collection("users").document(current.uid).set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            }

            if (name.isNotBlank() || !photoUrl.isNullOrBlank()) {
                val builder = UserProfileChangeRequest.Builder()
                if (name.isNotBlank()) builder.setDisplayName(name)
                if (!photoUrl.isNullOrBlank()) builder.setPhotoUri(Uri.parse(photoUrl))
                current.updateProfile(builder.build()).await()
            }

            syncAuthorFieldsOnExistingPosts(
                firebaseFirestore = firebaseFirestore,
                authorId = current.uid,
                newUsername = name.takeIf { it.isNotBlank() },
                newPhotoUrl = photoUrl
            )

            getProfile()
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to update profile", e)
        }
    }

    suspend fun followUserByUsername(username: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val current = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        val normalized = username.trim()
        if (normalized.isBlank()) {
            return@withContext RepositoryResult.Error("Enter a username")
        }
        try {
            val targetSnapshot = firebaseFirestore.collection("users")
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
            firebaseFirestore.collection("users")
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

            firebaseFirestore.collection("users")
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

    suspend fun unfollowUserByUsername(username: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val current = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        val normalized = username.trim()
        if (normalized.isBlank()) {
            return@withContext RepositoryResult.Error("Enter a username")
        }
        try {
            val targetId = resolveUserIdByUsername(firebaseFirestore, normalized)
                ?: return@withContext RepositoryResult.Error("User not found")
            if (targetId == current.uid) {
                return@withContext RepositoryResult.Error("You cannot unfollow yourself")
            }
            firebaseFirestore.collection("users")
                .document(current.uid)
                .collection("following")
                .document(targetId)
                .delete()
                .await()
            firebaseFirestore.collection("users")
                .document(targetId)
                .collection("followers")
                .document(current.uid)
                .delete()
                .await()
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to unfollow user", e)
        }
    }

    suspend fun isFollowingByUsername(username: String): RepositoryResult<Boolean> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: return@withContext RepositoryResult.Success(false)
        val firebaseFirestore = firestore ?: return@withContext RepositoryResult.Success(false)
        val current = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Success(false)
        val normalized = username.trim()
        if (normalized.isBlank()) return@withContext RepositoryResult.Success(false)
        try {
            val targetId = resolveUserIdByUsername(firebaseFirestore, normalized)
                ?: return@withContext RepositoryResult.Success(false)
            if (targetId == current.uid) return@withContext RepositoryResult.Success(false)
            val doc = firebaseFirestore.collection("users")
                .document(current.uid)
                .collection("following")
                .document(targetId)
                .get()
                .await()
            RepositoryResult.Success(doc.exists())
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to check follow state", e)
        }
    }

    private suspend fun resolveUserIdByUsername(
        firebaseFirestore: FirebaseFirestore,
        username: String
    ): String? {
        var snap = firebaseFirestore.collection("users")
            .whereEqualTo("usernameLower", username.lowercase())
            .limit(1)
            .get()
            .await()
        if (snap.isEmpty) {
            snap = firebaseFirestore.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .await()
        }
        return snap.documents.firstOrNull()?.id
    }

    suspend fun likePost(postId: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val current = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        val postRef = firebaseFirestore.collection("posts").document(postId)
        val alreadyLiked = AtomicBoolean(false)
        try {
            firebaseFirestore.runTransaction { transaction ->
                val snap = transaction.get(postRef)
                if (!snap.exists()) {
                    throw com.google.firebase.firestore.FirebaseFirestoreException(
                        "Post not found",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND
                    )
                }
                val raw = snap.get("likedUserIds")
                val liked = when (raw) {
                    is List<*> -> raw.filterIsInstance<String>().toSet()
                    else -> emptySet()
                }
                if (current.uid in liked) {
                    alreadyLiked.set(true)
                    return@runTransaction
                }
                transaction.update(
                    postRef,
                    mapOf(
                        "likedUserIds" to FieldValue.arrayUnion(current.uid),
                        "likesCount" to FieldValue.increment(1)
                    )
                )
            }.await()

            if (alreadyLiked.get()) {
                return@withContext RepositoryResult.Error(MESSAGE_ALREADY_LIKED)
            }

            val local = postDao.getById(postId)
            if (local != null) {
                postDao.upsert(
                    local.copy(
                        likesCount = local.likesCount + 1,
                        likedByCurrentUser = true
                    )
                )
            }
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to like post", e)
        }
    }

    /**
     * Removes the current user's like from [postId]. Idempotent: returns success if the user
     * has not liked the post.
     */
    suspend fun unlikePost(postId: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val current = firebaseAuth.currentUser ?: return@withContext RepositoryResult.Error("Not signed in")
        val postRef = firebaseFirestore.collection("posts").document(postId)
        try {
            firebaseFirestore.runTransaction { transaction ->
                val snap = transaction.get(postRef)
                if (!snap.exists()) return@runTransaction
                val raw = snap.get("likedUserIds")
                val liked = when (raw) {
                    is List<*> -> raw.filterIsInstance<String>().toSet()
                    else -> emptySet()
                }
                if (current.uid !in liked) return@runTransaction
                transaction.update(
                    postRef,
                    mapOf(
                        "likedUserIds" to FieldValue.arrayRemove(current.uid),
                        "likesCount" to FieldValue.increment(-1)
                    )
                )
            }.await()

            val local = postDao.getById(postId)
            if (local != null && local.likedByCurrentUser) {
                postDao.upsert(
                    local.copy(
                        likesCount = (local.likesCount - 1).coerceAtLeast(0),
                        likedByCurrentUser = false
                    )
                )
            }
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to unlike post", e)
        }
    }

    /** Like or unlike based on the current local state. Toggles atomically. */
    suspend fun toggleLikePost(postId: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val local = postDao.getById(postId)
        return@withContext if (local?.likedByCurrentUser == true) {
            unlikePost(postId)
        } else {
            likePost(postId)
        }
    }

    suspend fun getPublicProfileByUsername(username: String): RepositoryResult<PublicUserProfile> =
        withContext(Dispatchers.IO) {
            val firebaseFirestore = firestore
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val normalized = username.trim()
            if (normalized.isBlank()) return@withContext RepositoryResult.Error("Invalid username")
            try {
                var snap = firebaseFirestore.collection("users")
                    .whereEqualTo("usernameLower", normalized.lowercase())
                    .limit(1)
                    .get()
                    .await()
                if (snap.isEmpty) {
                    snap = firebaseFirestore.collection("users")
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
                        firebaseFirestore.collection("users").document(uid).collection("followers").get().await()
                    }
                    val followingD = async {
                        firebaseFirestore.collection("users").document(uid).collection("following").get().await()
                    }
                    val postsD = async {
                        firebaseFirestore.collection("posts")
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
        val firebaseFirestore = firestore
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        if (userId.isBlank()) return@withContext RepositoryResult.Error("Invalid user")
        try {
            val viewerUid = auth?.currentUser?.uid
            val posts = fetchPostsByAuthor(firebaseFirestore, userId, viewerUid)
            RepositoryResult.Success(posts)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load posts", e)
        }
    }

    /**
     * Loads posts authored by [userId], sorted newest-first.
     *
     * Firestore requires a composite index for `where(authorId) + orderBy(createdAt desc)`.
     * If the index is missing (cold-start of a fresh project), we still want the screen
     * to work, so we fall back to a query without `orderBy` and sort the (small, limit=50)
     * result set client-side.
     */
    private suspend fun fetchPostsByAuthor(
        firebaseFirestore: FirebaseFirestore,
        userId: String,
        viewerUid: String?
    ): List<Post> {
        val docs = try {
            firebaseFirestore.collection("posts")
                .whereEqualTo("authorId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
                .documents
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                // Composite index not yet created — fetch without orderBy and sort locally.
                firebaseFirestore.collection("posts")
                    .whereEqualTo("authorId", userId)
                    .limit(50)
                    .get()
                    .await()
                    .documents
                    .sortedByDescending { it.getLong("createdAt") ?: 0L }
            } else {
                throw e
            }
        }
        return docs.mapNotNull { it.toPost(currentUserId = viewerUid) }
    }

    suspend fun getFavoriteSymbolList(): RepositoryResult<List<String>> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: return@withContext RepositoryResult.Success(emptyList())
        val firebaseFirestore = firestore ?: return@withContext RepositoryResult.Success(emptyList())
        val uid = firebaseAuth.currentUser?.uid ?: return@withContext RepositoryResult.Success(emptyList())
        try {
            val snap = firebaseFirestore.collection("users")
                .document(uid)
                .collection("favoriteSymbols")
                .get()
                .await()
            val ids = snap.documents.map { it.id.uppercase() }.sorted()
            RepositoryResult.Success(ids)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load favorites", e)
        }
    }

    suspend fun isSymbolFavorite(symbol: String): Boolean = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: return@withContext false
        val firebaseFirestore = firestore ?: return@withContext false
        val uid = firebaseAuth.currentUser?.uid ?: return@withContext false
        val sym = symbol.trim().uppercase()
        if (sym.isBlank()) return@withContext false
        try {
            firebaseFirestore.collection("users")
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
            val firebaseAuth = auth
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val firebaseFirestore = firestore
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            val uid = firebaseAuth.currentUser?.uid ?: return@withContext RepositoryResult.Error("Not signed in")
            val sym = symbol.trim().uppercase()
            if (sym.isBlank()) return@withContext RepositoryResult.Error("Invalid symbol")
            try {
                val ref = firebaseFirestore.collection("users")
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

    private suspend fun syncAuthorFieldsOnExistingPosts(
        firebaseFirestore: FirebaseFirestore,
        authorId: String,
        newUsername: String?,
        newPhotoUrl: String?
    ) {
        if (newUsername.isNullOrBlank() && newPhotoUrl.isNullOrBlank()) return
        try {
            val updates = mutableMapOf<String, Any>()
            if (!newUsername.isNullOrBlank()) updates["authorUsername"] = newUsername
            if (!newPhotoUrl.isNullOrBlank()) updates["authorPhotoUrl"] = newPhotoUrl
            if (updates.isEmpty()) return

            val snapshot = firebaseFirestore.collection("posts")
                .whereEqualTo("authorId", authorId)
                .limit(MAX_POSTS_PER_SYNC)
                .get()
                .await()
            if (snapshot.isEmpty) return

            snapshot.documents.chunked(FIRESTORE_BATCH_LIMIT).forEach { chunk ->
                val batch = firebaseFirestore.batch()
                chunk.forEach { doc -> batch.update(doc.reference, updates) }
                batch.commit().await()
            }
        } catch (_: Exception) {
            // Best-effort sync; failures here should not break profile update.
        }
    }

    companion object {
        private const val FIRESTORE_BATCH_LIMIT = 400
        private const val MAX_POSTS_PER_SYNC: Long = 500

        const val MESSAGE_ALREADY_LIKED = "ALREADY_LIKED"

        private const val FIREBASE_NOT_CONFIGURED_MESSAGE =
            "Firebase is not configured on this build. Add app/google-services.json to enable profile actions."
    }
}

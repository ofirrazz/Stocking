package com.stocksocial.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal object UserFirestoreHelper {

    suspend fun ensureUserProfileDocument(
        firestore: FirebaseFirestore,
        user: FirebaseUser,
        preferredUsername: String? = null
    ): DocumentSnapshot {
        val ref = firestore.collection("users").document(user.uid)
        val existing = ref.get().await()
        if (existing.exists()) return existing

        val trimmedName = preferredUsername?.trim()?.takeIf { it.isNotEmpty() }
            ?: user.displayName?.trim()?.takeIf { it.isNotEmpty() }
            ?: user.email?.substringBefore("@")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "user"
        val photo = user.photoUrl?.toString().orEmpty()
        val userDoc = hashMapOf(
            "username" to trimmedName,
            "usernameLower" to trimmedName.lowercase(),
            "displayName" to trimmedName,
            "email" to user.email.orEmpty(),
            "photoUrl" to photo,
            "bio" to "",
            "location" to "",
            "website" to "",
            "bannerUrl" to "",
            "createdAt" to System.currentTimeMillis()
        )
        ref.set(userDoc).await()
        return ref.get().await()
    }

    /**
     * Result of a username availability check.
     *
     * We intentionally separate "available" from "could not verify" so that the caller can
     * decide whether to block registration or surface a clear error to the user.
     */
    sealed class UsernameAvailability {
        object Available : UsernameAvailability()
        object Taken : UsernameAvailability()
        data class Unknown(val cause: Exception) : UsernameAvailability()
    }

    /**
     * Checks whether [username] is already taken. The lookup uses the lowercase index field
     * (`usernameLower`) and falls back to the exact-case field if needed.
     */
    suspend fun checkUsernameAvailability(
        firestore: FirebaseFirestore,
        username: String,
        excludeUid: String? = null
    ): UsernameAvailability {
        val trimmed = username.trim()
        val lower = trimmed.lowercase()
        if (lower.isEmpty()) return UsernameAvailability.Available
        val snapshot = try {
            firestore.collection("users")
                .whereEqualTo("usernameLower", lower)
                .limit(1)
                .get()
                .await()
        } catch (primary: Exception) {
            try {
                firestore.collection("users")
                    .whereEqualTo("username", trimmed)
                    .limit(1)
                    .get()
                    .await()
            } catch (fallback: Exception) {
                return UsernameAvailability.Unknown(fallback)
            }
        }
        val doc = snapshot.documents.firstOrNull() ?: return UsernameAvailability.Available
        return if (excludeUid == null || doc.id != excludeUid) {
            UsernameAvailability.Taken
        } else {
            UsernameAvailability.Available
        }
    }

    /**
     * Legacy boolean wrapper. Prefer [checkUsernameAvailability] in new code; this helper
     * keeps the old behavior (`Unknown` -> `false`) for callers that don't differentiate.
     */
    suspend fun isUsernameTaken(
        firestore: FirebaseFirestore,
        username: String,
        excludeUid: String? = null
    ): Boolean = when (checkUsernameAvailability(firestore, username, excludeUid)) {
        UsernameAvailability.Taken -> true
        UsernameAvailability.Available -> false
        is UsernameAvailability.Unknown -> false
    }

    suspend fun resolveUsername(
        firestore: FirebaseFirestore,
        uid: String,
        fallback: String
    ): String = try {
        val doc = firestore.collection("users").document(uid).get().await()
        doc.getString("username")?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
    } catch (_: Exception) {
        fallback
    }

    suspend fun resolvePhotoUrl(
        firestore: FirebaseFirestore,
        uid: String,
        fallback: String? = null
    ): String? = try {
        val doc = firestore.collection("users").document(uid).get().await()
        doc.getString("photoUrl")?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
    } catch (_: Exception) {
        fallback
    }
}

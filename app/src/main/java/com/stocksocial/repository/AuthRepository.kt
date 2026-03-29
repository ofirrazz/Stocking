package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.stocksocial.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun login(email: String, password: String): RepositoryResult<User> =
        withContext(Dispatchers.IO) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val u = result.user ?: return@withContext RepositoryResult.Error("Sign-in failed")
                RepositoryResult.Success(mapFirebaseUser(u))
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Login failed", e)
            }
        }

    suspend fun register(username: String, email: String, password: String): RepositoryResult<User> =
        withContext(Dispatchers.IO) {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val u = result.user ?: return@withContext RepositoryResult.Error("Registration failed")
                u.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(username).build()
                ).await()
                val userDoc = hashMapOf(
                    "username" to username,
                    "email" to email,
                    "photoUrl" to "",
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("users").document(u.uid).set(userDoc).await()
                RepositoryResult.Success(
                    User(
                        id = u.uid,
                        username = username,
                        email = email,
                        avatarUrl = null,
                        bio = null
                    )
                )
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Registration failed", e)
            }
        }

    private suspend fun mapFirebaseUser(u: FirebaseUser): User {
        val doc = firestore.collection("users").document(u.uid).get().await()
        val username = doc.getString("username")
            ?: u.displayName
            ?: u.email?.substringBefore("@")
            ?: "user"
        return User(
            id = u.uid,
            username = username,
            email = u.email.orEmpty(),
            avatarUrl = doc.getString("photoUrl")?.takeIf { it.isNotBlank() },
            bio = doc.getString("bio")
        )
    }

    fun logout() {
        auth.signOut()
    }
}

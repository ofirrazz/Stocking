package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.stocksocial.model.User
import com.stocksocial.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val tokenManager: TokenManager
) {
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): RepositoryResult<User> = withContext(Dispatchers.IO) {
        runCatching {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: error("Firebase user is null")
            val profileRequest = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()
            firebaseUser.updateProfile(profileRequest).await()
            saveSession(firebaseUser)
            firebaseUser.toDomainUser()
        }.fold(
            onSuccess = { RepositoryResult.Success(it) },
            onFailure = { RepositoryResult.Error(it.message ?: "Registration failed", it) }
        )
    }

    suspend fun login(
        email: String,
        password: String
    ): RepositoryResult<User> = withContext(Dispatchers.IO) {
        runCatching {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: error("Firebase user is null")
            saveSession(firebaseUser)
            firebaseUser.toDomainUser()
        }.fold(
            onSuccess = { RepositoryResult.Success(it) },
            onFailure = { RepositoryResult.Error(it.message ?: "Login failed", it) }
        )
    }

    fun getCurrentUser(): User? {
        val user = runCatching { firebaseAuth.currentUser }.getOrNull() ?: return null
        return user.toDomainUser()
    }

    fun isLoggedIn(): Boolean = firebaseAuth.currentUser != null || tokenManager.isLoggedIn()
    fun getCurrentUserId(): String = getCurrentUser()?.id ?: tokenManager.getUserId() ?: "local-user"
    fun getCurrentUsername(): String = getCurrentUser()?.username ?: tokenManager.getUsername() ?: "StockSocial User"

    fun logout() {
        firebaseAuth.signOut()
        tokenManager.clearToken()
    }

    private suspend fun saveSession(firebaseUser: FirebaseUser) {
        val idToken = firebaseUser.getIdToken(false).await().token ?: "firebase-${firebaseUser.uid}"
        tokenManager.saveSession(
            token = idToken,
            userId = firebaseUser.uid,
            username = firebaseUser.displayName ?: firebaseUser.email ?: "StockSocial User"
        )
    }

    private fun FirebaseUser.toDomainUser(): User {
        return User(
            id = uid,
            username = displayName ?: email?.substringBefore("@").orEmpty(),
            email = email.orEmpty(),
            avatarUrl = photoUrl?.toString()
        )
    }
}

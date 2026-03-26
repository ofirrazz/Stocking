package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.stocksocial.model.auth.LoginRequest
import com.stocksocial.model.auth.LoginResponse
import com.stocksocial.model.auth.RegisterRequest
import com.stocksocial.model.auth.RegisterResponse
import com.stocksocial.network.ApiService
import com.stocksocial.network.RetrofitClient
import com.stocksocial.utils.TokenManager
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val tokenManager: TokenManager? = null
) {
    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun login(email: String, pass: String): FirebaseUser? {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await().user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun register(email: String, pass: String): FirebaseUser? {
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await().user
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        auth.signOut()
        tokenManager?.clearToken()
    }

    suspend fun loginWithApi(email: String, pass: String): RepositoryResult<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(email = email, password = pass))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                tokenManager?.saveToken(body.token)
                RepositoryResult.Success(body)
            } else {
                RepositoryResult.Error("API login failed: ${response.code()}")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Network error during API login", e)
        }
    }

    suspend fun registerWithApi(
        email: String,
        pass: String,
        username: String
    ): RepositoryResult<RegisterResponse> {
        return try {
            val response = apiService.register(
                RegisterRequest(email = email, password = pass, username = username)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                tokenManager?.saveToken(body.token)
                RepositoryResult.Success(body)
            } else {
                RepositoryResult.Error("API register failed: ${response.code()}")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Network error during API register", e)
        }
    }
}

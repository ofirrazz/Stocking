package com.stocksocial.repository

import com.stocksocial.model.auth.AuthResponse
import com.stocksocial.model.auth.LoginRequest
import com.stocksocial.model.auth.RefreshTokenRequest
import com.stocksocial.model.auth.RefreshTokenResponse
import com.stocksocial.model.auth.RegisterRequest
import com.stocksocial.network.ApiService
import com.stocksocial.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    suspend fun login(
        email: String,
        password: String
    ): RepositoryResult<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            apiService.login(LoginRequest(email = email, password = password))
        }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    tokenManager.saveToken(body.accessToken)
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Login failed: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("Login request failed", throwable)
            }
        )
    }

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): RepositoryResult<AuthResponse> = withContext(Dispatchers.IO) {
        runCatching {
            apiService.register(
                RegisterRequest(
                    username = username,
                    email = email,
                    password = password
                )
            )
        }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    tokenManager.saveToken(body.accessToken)
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Registration failed: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("Registration request failed", throwable)
            }
        )
    }

    suspend fun refreshToken(
        refreshToken: String
    ): RepositoryResult<RefreshTokenResponse> = withContext(Dispatchers.IO) {
        runCatching {
            apiService.refreshToken(RefreshTokenRequest(refreshToken = refreshToken))
        }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    tokenManager.saveToken(body.accessToken)
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Token refresh failed: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("Token refresh request failed", throwable)
            }
        )
    }

    fun getSavedToken(): String? = tokenManager.getToken()

    fun logout() {
        tokenManager.clearToken()
    }
}

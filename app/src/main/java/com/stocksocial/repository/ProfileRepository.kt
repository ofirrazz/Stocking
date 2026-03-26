package com.stocksocial.repository

import com.stocksocial.model.User
import com.stocksocial.network.ApiService
import com.stocksocial.network.RetrofitClient

class ProfileRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) {
    suspend fun getProfile(userId: String): RepositoryResult<User> {
        return try {
            val response = apiService.getUserProfile(userId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                RepositoryResult.Success(body)
            } else {
                RepositoryResult.Error("Failed to fetch profile: ${response.code()}")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Network error while fetching profile", e)
        }
    }
}

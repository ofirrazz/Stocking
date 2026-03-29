package com.stocksocial.repository

import com.stocksocial.model.Post
import com.stocksocial.model.User
import com.stocksocial.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileRepository(
    private val apiService: ApiService
) {

    suspend fun getProfile(): RepositoryResult<User> = withContext(Dispatchers.IO) {
        runCatching { apiService.getMyProfile() }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Failed to fetch profile: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("Profile request failed", throwable)
            }
        )
    }

    suspend fun getMyPosts(): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getMyPosts() }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Failed to fetch user posts: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("User posts request failed", throwable)
            }
        )
    }
}

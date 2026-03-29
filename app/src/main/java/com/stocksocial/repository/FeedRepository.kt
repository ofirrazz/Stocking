package com.stocksocial.repository

import com.stocksocial.model.Post
import com.stocksocial.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedRepository(
    private val apiService: ApiService
) {

    suspend fun getFeedPosts(): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getFeedPosts() }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Failed to fetch feed: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("Feed request failed", throwable)
            }
        )
    }
}

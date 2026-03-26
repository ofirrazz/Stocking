package com.stocksocial.repository

import com.stocksocial.model.WatchlistItem
import com.stocksocial.network.ApiService
import com.stocksocial.network.RetrofitClient

class WatchlistRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) {
    suspend fun getWatchlist(userId: String): RepositoryResult<List<WatchlistItem>> {
        return try {
            val response = apiService.getWatchlist(userId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                RepositoryResult.Success(body)
            } else {
                RepositoryResult.Error("Failed to fetch watchlist: ${response.code()}")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Network error while fetching watchlist", e)
        }
    }
}

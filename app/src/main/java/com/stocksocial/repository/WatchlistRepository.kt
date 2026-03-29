package com.stocksocial.repository

import com.stocksocial.model.WatchlistItem
import com.stocksocial.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WatchlistRepository(
    private val apiService: ApiService
) {

    suspend fun getWatchlist(): RepositoryResult<List<WatchlistItem>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getWatchlist() }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Failed to fetch watchlist: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("Watchlist request failed", throwable)
            }
        )
    }
}

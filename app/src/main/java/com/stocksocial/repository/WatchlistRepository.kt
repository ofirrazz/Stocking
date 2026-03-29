package com.stocksocial.repository

import com.stocksocial.model.WatchlistItem
import com.stocksocial.utils.DummyData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WatchlistRepository {

    suspend fun getWatchlist(): RepositoryResult<List<WatchlistItem>> = withContext(Dispatchers.IO) {
        val stocks = DummyData.watchlistStocks()
        val items = stocks.mapIndexed { index, stock ->
            WatchlistItem(
                id = "local_$index",
                userId = "local",
                stock = stock,
                createdAt = ""
            )
        }
        RepositoryResult.Success(items)
    }
}

package com.stocksocial.repository

import com.stocksocial.BuildConfig
import com.stocksocial.model.Stock
import com.stocksocial.network.currentDisplayPrice
import com.stocksocial.model.WatchlistItem
import com.stocksocial.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class WatchlistRepository(
    private val apiService: ApiService
) {

    suspend fun getWatchlist(): RepositoryResult<List<WatchlistItem>> = withContext(Dispatchers.IO) {
        if (BuildConfig.FINNHUB_TOKEN.isBlank()) {
            return@withContext RepositoryResult.Error("Add FINNHUB_TOKEN in local.properties (see README).")
        }
        try {
            val stocks = fetchStocks(WATCHLIST_SYMBOLS)
            val items = stocks.mapIndexed { index, stock ->
                WatchlistItem(
                    id = "remote_$index",
                    userId = "remote",
                    stock = stock,
                    createdAt = ""
                )
            }
            RepositoryResult.Success(items)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load watchlist", e)
        }
    }

    suspend fun getMarketIndices(): RepositoryResult<List<Stock>> = withContext(Dispatchers.IO) {
        if (BuildConfig.FINNHUB_TOKEN.isBlank()) {
            return@withContext RepositoryResult.Error("Add FINNHUB_TOKEN in local.properties (see README).")
        }
        try {
            RepositoryResult.Success(fetchStocks(MARKET_SYMBOLS))
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load market indices", e)
        }
    }

    suspend fun getTrendingStocks(): RepositoryResult<List<Stock>> = withContext(Dispatchers.IO) {
        if (BuildConfig.FINNHUB_TOKEN.isBlank()) {
            return@withContext RepositoryResult.Error("Add FINNHUB_TOKEN in local.properties (see README).")
        }
        try {
            RepositoryResult.Success(fetchStocks(TRENDING_SYMBOLS))
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load trending stocks", e)
        }
    }

    suspend fun getStockBySymbol(symbol: String): RepositoryResult<Stock> = withContext(Dispatchers.IO) {
        if (BuildConfig.FINNHUB_TOKEN.isBlank()) {
            return@withContext RepositoryResult.Error("Add FINNHUB_TOKEN in local.properties (see README).")
        }
        val normalized = symbol.trim().uppercase()
        if (normalized.isBlank()) {
            return@withContext RepositoryResult.Error("Enter a stock symbol")
        }
        try {
            val response = apiService.getQuote(symbol = normalized)
            val body = response.body()
            val price = body?.currentDisplayPrice()
            if (!response.isSuccessful || body == null || price == null) {
                return@withContext RepositoryResult.Error("Stock symbol not found")
            }
            val pct = if (body.c > 0) body.dp else 0.0
            RepositoryResult.Success(
                Stock(
                    symbol = normalized,
                    name = SYMBOL_NAMES[normalized] ?: normalized,
                    price = price,
                    dailyChangePercent = pct
                )
            )
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load stock", e)
        }
    }

    private suspend fun fetchStocks(symbols: List<String>): List<Stock> = coroutineScope {
        symbols.map { symbol ->
            async {
                val response = apiService.getQuote(symbol = symbol)
                val body = response.body()
                val price = body?.currentDisplayPrice()
                if (!response.isSuccessful || body == null || price == null) {
                    return@async null
                }
                val pct = if (body.c > 0) body.dp else 0.0
                Stock(
                    symbol = symbol,
                    name = SYMBOL_NAMES[symbol] ?: symbol,
                    price = price,
                    dailyChangePercent = pct
                )
            }
        }.awaitAll().filterNotNull()
    }

    suspend fun getLatestPrices(symbols: List<String>): Map<String, Double> = withContext(Dispatchers.IO) {
        if (BuildConfig.FINNHUB_TOKEN.isBlank()) return@withContext emptyMap()
        val distinct = symbols.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()
        if (distinct.isEmpty()) return@withContext emptyMap()
        coroutineScope {
            distinct.map { symbol ->
                async {
                    try {
                        val response = apiService.getQuote(symbol = symbol)
                        val body = response.body()
                        val price = body?.currentDisplayPrice() ?: return@async null
                        symbol to price
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }

    companion object {
        private val MARKET_SYMBOLS = listOf("SPY", "DIA", "QQQ")
        private val TRENDING_SYMBOLS = listOf("NVDA", "AAPL", "TSLA", "MSFT", "GOOGL", "AMD")
        private val WATCHLIST_SYMBOLS = listOf("AAPL", "NVDA", "MSFT", "TSLA")

        private val SYMBOL_NAMES = mapOf(
            "SPY" to "S&P 500 ETF",
            "DIA" to "Dow Jones ETF",
            "QQQ" to "Nasdaq 100 ETF",
            "NVDA" to "NVIDIA",
            "AMD" to "Advanced Micro Devices",
            "MSFT" to "Microsoft",
            "AAPL" to "Apple",
            "TSLA" to "Tesla",
            "GOOGL" to "Alphabet"
        )
    }
}

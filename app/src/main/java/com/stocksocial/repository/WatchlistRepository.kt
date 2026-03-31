package com.stocksocial.repository

import com.stocksocial.BuildConfig
import com.stocksocial.model.Stock
import com.stocksocial.model.SymbolSearchHit
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

    suspend fun getStocksForSymbols(symbols: List<String>): RepositoryResult<List<Stock>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.FINNHUB_TOKEN.isBlank()) {
                return@withContext RepositoryResult.Error("Add FINNHUB_TOKEN in local.properties (see README).")
            }
            val normalized = symbols.map { it.trim().uppercase() }.filter { it.isNotEmpty() }
            if (normalized.isEmpty()) {
                return@withContext RepositoryResult.Success(emptyList())
            }
            try {
                RepositoryResult.Success(fetchStocks(normalized))
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Failed to load stocks", e)
            }
        }

    suspend fun searchSymbols(query: String): RepositoryResult<List<SymbolSearchHit>> = withContext(Dispatchers.IO) {
        if (BuildConfig.FINNHUB_TOKEN.isBlank()) {
            return@withContext RepositoryResult.Error("Add FINNHUB_TOKEN in local.properties (see README).")
        }
        val q = query.trim()
        if (q.length < 1) {
            return@withContext RepositoryResult.Success(emptyList())
        }
        try {
            val response = apiService.searchSymbols(query = q)
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return@withContext RepositoryResult.Success(emptyList())
            }
            val normQ = q.uppercase()
            val seen = mutableSetOf<String>()
            val filtered = body.result.orEmpty().mapNotNull { item ->
                val rawSym = (item.displaySymbol ?: item.symbol)?.trim()?.uppercase().orEmpty()
                if (rawSym.isEmpty() || rawSym in seen) return@mapNotNull null
                val t = item.type?.lowercase().orEmpty()
                val allowedType = t.isBlank() ||
                    t.contains("common") ||
                    t.contains("etf") ||
                    t.contains("adr") ||
                    t.contains("crypto")
                if (!allowedType) return@mapNotNull null
                seen.add(rawSym)
                val desc = item.description?.trim()?.takeIf { it.isNotEmpty() } ?: rawSym
                SymbolSearchHit(symbol = rawSym, description = desc)
            }
            val hits = filtered
                .sortedWith(
                    compareBy<SymbolSearchHit>(
                        { if (it.symbol == normQ) 0 else 1 },
                        { if (it.symbol.startsWith(normQ)) 0 else 1 },
                        { if (it.description.uppercase().startsWith(normQ)) 0 else 1 },
                        { it.symbol.length }
                    )
                )
                .take(20)
            RepositoryResult.Success(hits)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Search failed", e)
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
            "GOOGL" to "Alphabet",
            "META" to "Meta",
            "JPM" to "JPMorgan Chase",
            "BAC" to "Bank of America",
            "WFC" to "Wells Fargo",
            "GS" to "Goldman Sachs",
            "C" to "Citigroup",
            "MS" to "Morgan Stanley",
            "BINANCE:BTCUSDT" to "Bitcoin",
            "BINANCE:ETHUSDT" to "Ethereum",
            "BINANCE:SOLUSDT" to "Solana",
            "BINANCE:XRPUSDT" to "XRP",
            "BINANCE:ADAUSDT" to "Cardano"
        )
    }
}

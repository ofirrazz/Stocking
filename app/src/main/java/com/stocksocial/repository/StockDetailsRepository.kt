package com.stocksocial.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.stocksocial.data.remote.toPost
import com.stocksocial.model.AnalystRecommendation
import com.stocksocial.model.Post
import com.stocksocial.model.PriceChartSeries
import com.stocksocial.model.Stock
import com.stocksocial.network.ApiService
import com.stocksocial.network.currentDisplayPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class StockDetailsRepository(
    private val apiService: ApiService,
    private val firestore: FirebaseFirestore
) {
    suspend fun getPriceHistory(symbol: String): RepositoryResult<PriceChartSeries> = withContext(Dispatchers.IO) {
        try {
            val normalized = symbol.trim().uppercase()
            val nowSec = System.currentTimeMillis() / 1000
            val fromSec = nowSec - 60L * 60 * 24 * 30
            val response = apiService.getCandles(
                symbol = normalized,
                resolution = "D",
                from = fromSec,
                to = nowSec
            )
            val body = response.body()
                ?: return@withContext RepositoryResult.Error("Chart data unavailable")
            if (body.s != "ok" || body.c.isEmpty() || body.t.isEmpty()) {
                return@withContext RepositoryResult.Error("Chart data unavailable")
            }
            val zipped = body.t.zip(body.c)
            val points = zipped.takeLast(30)
            val lastVol = when {
                body.v.isNotEmpty() && body.v.size == body.c.size -> body.v.last()
                body.v.isNotEmpty() -> body.v.last()
                else -> null
            }
            RepositoryResult.Success(PriceChartSeries(points = points, lastVolume = lastVol))
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load chart", e)
        }
    }

    suspend fun getLiveQuote(symbol: String): RepositoryResult<Stock> = withContext(Dispatchers.IO) {
        try {
            val normalized = symbol.trim().uppercase()
            val response = apiService.getQuote(normalized)
            val body = response.body()
                ?: return@withContext RepositoryResult.Error("Quote unavailable")
            val price = body.currentDisplayPrice()
                ?: return@withContext RepositoryResult.Error("Quote unavailable")
            val pct = if (body.c > 0) body.dp else 0.0
            RepositoryResult.Success(
                Stock(
                    symbol = normalized,
                    name = SYMBOL_NAMES[normalized] ?: normalized,
                    price = price,
                    dailyChangePercent = pct,
                    open = body.o,
                    high = body.h,
                    low = body.l,
                    previousClose = body.pc,
                    dayChangeAbs = body.d
                )
            )
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load quote", e)
        }
    }

    suspend fun getAnalystRecommendations(symbol: String): RepositoryResult<List<AnalystRecommendation>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRecommendationTrends(symbol.trim().uppercase())
                val body = response.body().orEmpty()
                val mapped = body.map {
                    AnalystRecommendation(
                        period = it.period,
                        buy = it.buy,
                        hold = it.hold,
                        sell = it.sell,
                        strongBuy = it.strongBuy,
                        strongSell = it.strongSell
                    )
                }
                RepositoryResult.Success(mapped)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Failed to load recommendations", e)
            }
        }

    suspend fun getPostsForSymbol(symbol: String): RepositoryResult<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val normalized = symbol.trim().uppercase()
            val byField = firestore.collection("posts")
                .whereEqualTo("stockSymbol", normalized)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()
                .documents
                .mapNotNull { it.toPost() }

            if (byField.isNotEmpty()) {
                return@withContext RepositoryResult.Success(byField)
            }

            val allRecent = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(80)
                .get()
                .await()
                .documents
                .mapNotNull { it.toPost() }
                .filter { post ->
                    post.content.contains("$$normalized", ignoreCase = true) ||
                        post.stockSymbol.equals(normalized, ignoreCase = true)
                }
            RepositoryResult.Success(allRecent)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to load stock posts", e)
        }
    }

    companion object {
        fun symbolNameOrTicker(symbol: String): String =
            SYMBOL_NAMES[symbol.trim().uppercase()] ?: symbol.trim().uppercase()

        private val SYMBOL_NAMES = mapOf(
            "SPY" to "S&P 500 ETF",
            "DIA" to "Dow Jones ETF",
            "QQQ" to "Nasdaq 100 ETF",
            "NVDA" to "NVIDIA",
            "AMD" to "Advanced Micro Devices",
            "MSFT" to "Microsoft",
            "AAPL" to "Apple Inc.",
            "TSLA" to "Tesla",
            "GOOGL" to "Alphabet Inc.",
            "META" to "Meta Platforms",
            "AMZN" to "Amazon",
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

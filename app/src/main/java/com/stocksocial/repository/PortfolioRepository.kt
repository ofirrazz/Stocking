package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stocksocial.model.PortfolioHolding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PortfolioRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val watchlistRepository: WatchlistRepository
) {

    suspend fun getHoldings(): RepositoryResult<List<PortfolioHolding>> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext RepositoryResult.Error("Not signed in")
        loadHoldingsForUserId(uid)
    }

    suspend fun getHoldingsForUser(userId: String): RepositoryResult<List<PortfolioHolding>> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext RepositoryResult.Error("Invalid user")
        loadHoldingsForUserId(userId)
    }

    private suspend fun loadHoldingsForUserId(uid: String): RepositoryResult<List<PortfolioHolding>> {
        try {
            val docs = firestore.collection("users")
                .document(uid)
                .collection("portfolio")
                .get()
                .await()
                .documents

            val holdings = docs.mapNotNull { doc ->
                val symbol = doc.getString("symbol")?.uppercase().orEmpty()
                val shares = (doc.getDouble("shares") ?: 0.0)
                val buyPrice = (doc.getDouble("buyPrice") ?: 0.0)
                if (symbol.isBlank() || shares <= 0.0 || buyPrice <= 0.0) return@mapNotNull null
                val quote = when (val q = watchlistRepository.getStockBySymbol(symbol)) {
                    is RepositoryResult.Success -> q.data.price
                    is RepositoryResult.Error -> buyPrice
                }
                PortfolioHolding(symbol, shares, buyPrice, quote)
            }
            return RepositoryResult.Success(holdings.sortedBy { it.symbol })
        } catch (e: Exception) {
            return RepositoryResult.Error(e.message ?: "Failed to load portfolio", e)
        }
    }

    suspend fun upsertHolding(symbol: String, shares: Double, buyPrice: Double): RepositoryResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@withContext RepositoryResult.Error("Not signed in")
            val normalized = symbol.trim().uppercase()
            if (normalized.isBlank() || shares <= 0.0 || buyPrice <= 0.0) {
                return@withContext RepositoryResult.Error("Enter valid symbol, shares and buy price")
            }
            try {
                firestore.collection("users")
                    .document(uid)
                    .collection("portfolio")
                    .document(normalized)
                    .set(
                        mapOf(
                            "symbol" to normalized,
                            "shares" to shares,
                            "buyPrice" to buyPrice,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
                RepositoryResult.Success(Unit)
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Failed to save holding", e)
            }
        }
}

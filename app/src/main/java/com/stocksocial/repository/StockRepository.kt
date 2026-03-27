package com.stocksocial.repository

import com.stocksocial.model.Stock
import com.stocksocial.network.ApiService
import com.stocksocial.network.RetrofitClient

class StockRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) {
    suspend fun searchStocks(query: String): RepositoryResult<List<Stock>> {
        return try {
            val response = apiService.searchStocks(query)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                RepositoryResult.Success(body)
            } else {
                RepositoryResult.Error("Failed to search stocks: ${response.code()}")
            }
        } catch (e: Exception) {
            RepositoryResult.Error("Network error while searching stocks", e)
        }
    }
}

package com.stocksocial.repository

import com.stocksocial.model.Article
import com.stocksocial.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArticlesRepository(
    private val apiService: ApiService
) {

    suspend fun getArticles(): RepositoryResult<List<Article>> = withContext(Dispatchers.IO) {
        runCatching { apiService.getArticles() }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Failed to fetch articles: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("Articles request failed", throwable)
            }
        )
    }

    suspend fun getArticleById(articleId: String): RepositoryResult<Article> = withContext(Dispatchers.IO) {
        runCatching { apiService.getArticleById(articleId) }.fold(
            onSuccess = { response ->
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    RepositoryResult.Success(body)
                } else {
                    RepositoryResult.Error("Failed to fetch article details: ${response.code()}")
                }
            },
            onFailure = { throwable ->
                RepositoryResult.Error("Article details request failed", throwable)
            }
        )
    }
}

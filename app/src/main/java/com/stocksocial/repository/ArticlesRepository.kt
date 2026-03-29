package com.stocksocial.repository

import android.content.Context
import com.stocksocial.BuildConfig
import com.stocksocial.data.local.ArticleDao
import com.stocksocial.model.Article
import com.stocksocial.model.cache.CachedArticleEntity
import com.stocksocial.model.cache.toArticle
import com.stocksocial.model.cache.toEntity
import com.stocksocial.network.ApiService
import com.stocksocial.network.toArticle
import com.stocksocial.utils.Constants
import com.stocksocial.utils.ImageCacheDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ArticlesRepository(
    private val apiService: ApiService,
    private val articleDao: ArticleDao,
    private val appContext: Context
) {

    suspend fun getArticles(symbol: String = Constants.DEFAULT_NEWS_SYMBOL): RepositoryResult<List<Article>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.FINNHUB_TOKEN.isBlank()) {
                return@withContext cachedOrError("Add FINNHUB_TOKEN in local.properties (see README).")
            }
            try {
                val range = buildDateRange()
                val response = apiService.getCompanyNews(symbol = symbol, from = range.first, to = range.second)
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    return@withContext cachedOrError("News request failed: ${response.code()}")
                }
                val entities = body.map { it.toArticle().toEntity(null) }
                replaceCache(entities)
                hydrateArticleImages(entities)
                RepositoryResult.Success(getCachedArticles())
            } catch (e: Exception) {
                cachedOrError(e.message ?: "Network error", e)
            }
        }

    suspend fun getArticleById(articleId: String): RepositoryResult<Article> =
        withContext(Dispatchers.IO) {
            val row = articleDao.getById(articleId)
            if (row != null) {
                RepositoryResult.Success(row.toArticle())
            } else {
                RepositoryResult.Error("Article not found locally. Open the news list online first.")
            }
        }

    private suspend fun cachedOrError(
        message: String,
        throwable: Throwable? = null
    ): RepositoryResult<List<Article>> {
        val cached = getCachedArticles()
        return if (cached.isNotEmpty()) {
            RepositoryResult.Success(cached)
        } else {
            RepositoryResult.Error(message, throwable)
        }
    }

    private fun buildDateRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val toDate = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, -30)
        val fromDate = calendar.time
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(fromDate) to formatter.format(toDate)
    }

    private suspend fun replaceCache(entities: List<CachedArticleEntity>) {
        articleDao.replaceAll(entities)
    }

    private suspend fun hydrateArticleImages(entities: List<CachedArticleEntity>) {
        entities.forEach { entity ->
            val url = entity.imageUrl ?: return@forEach
            val path = ImageCacheDownloader.download(
                appContext,
                url,
                "article_images",
                entity.id.replace("/", "_") + ".img"
            ) ?: return@forEach
            articleDao.insert(entity.copy(localImagePath = path))
        }
    }

    suspend fun getCachedArticles(): List<Article> {
        return articleDao.getAll().map { it.toArticle() }
    }
}

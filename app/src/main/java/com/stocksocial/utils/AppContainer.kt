package com.stocksocial.utils

import android.content.Context
import com.stocksocial.local.StockSocialDatabase
import com.stocksocial.network.RetrofitInstance
import com.stocksocial.repository.ArticlesRepository
import com.stocksocial.repository.AuthRepository
import com.stocksocial.repository.FeedRepository
import com.stocksocial.repository.LocalPostsRepository
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.WatchlistRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val tokenManager = TokenManager(appContext)
    private val apiService = RetrofitInstance.createApiService(tokenManager)
    private val database = StockSocialDatabase.getInstance(appContext)

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            tokenManager = tokenManager
        )
    }

    val feedRepository: FeedRepository by lazy { FeedRepository(apiService) }
    val profileRepository: ProfileRepository by lazy { ProfileRepository(apiService) }
    val articlesRepository: ArticlesRepository by lazy { ArticlesRepository(apiService) }
    val watchlistRepository: WatchlistRepository by lazy { WatchlistRepository(apiService) }
    val localPostsRepository: LocalPostsRepository by lazy { LocalPostsRepository(database.postDao()) }
}

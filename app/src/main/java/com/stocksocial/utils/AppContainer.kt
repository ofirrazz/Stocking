package com.stocksocial.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.stocksocial.data.local.AppDatabase
import com.stocksocial.network.RetrofitInstance
import com.stocksocial.repository.ArticlesRepository
import com.stocksocial.repository.AuthRepository
import com.stocksocial.repository.FeedRepository
import com.stocksocial.repository.PortfolioRepository
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.StockDetailsRepository
import com.stocksocial.repository.WatchlistRepository
import com.stocksocial.viewmodel.AppViewModelFactory

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    private val apiService = RetrofitInstance.createApiService()

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance()
        )
    }

    val articlesRepository: ArticlesRepository by lazy {
        ArticlesRepository(
            apiService = apiService,
            articleDao = database.articleDao(),
            appContext = appContext
        )
    }

    val feedRepository: FeedRepository by lazy {
        FeedRepository(
            firestore = FirebaseFirestore.getInstance(),
            storage = FirebaseStorage.getInstance(),
            auth = FirebaseAuth.getInstance(),
            postDao = database.postDao(),
            appContext = appContext,
            watchlistRepository = watchlistRepository
        )
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            postDao = database.postDao(),
            storage = FirebaseStorage.getInstance()
        )
    }

    val watchlistRepository: WatchlistRepository by lazy { WatchlistRepository(apiService = apiService) }
    val stockDetailsRepository: StockDetailsRepository by lazy {
        StockDetailsRepository(
            apiService = apiService,
            firestore = FirebaseFirestore.getInstance()
        )
    }
    val portfolioRepository: PortfolioRepository by lazy {
        PortfolioRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            watchlistRepository = watchlistRepository
        )
    }

    val viewModelFactory: AppViewModelFactory by lazy {
        AppViewModelFactory(
            authRepository = authRepository,
            feedRepository = feedRepository,
            profileRepository = profileRepository,
            articlesRepository = articlesRepository,
            watchlistRepository = watchlistRepository,
            stockDetailsRepository = stockDetailsRepository,
            portfolioRepository = portfolioRepository
        )
    }
}

package com.stocksocial.utils

import android.content.Context
import com.stocksocial.data.prefs.RecentHotSearchStore
import com.google.firebase.FirebaseApp
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
    private val isFirebaseConfigured = FirebaseApp.getApps(appContext).isNotEmpty()

    private val firebaseAuth: FirebaseAuth? by lazy {
        if (isFirebaseConfigured) FirebaseAuth.getInstance() else null
    }
    private val firebaseFirestore: FirebaseFirestore? by lazy {
        if (isFirebaseConfigured) FirebaseFirestore.getInstance() else null
    }
    private val firebaseStorage: FirebaseStorage? by lazy {
        if (isFirebaseConfigured) FirebaseStorage.getInstance() else null
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            auth = firebaseAuth,
            firestore = firebaseFirestore
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
            firestore = firebaseFirestore,
            storage = firebaseStorage,
            auth = firebaseAuth,
            postDao = database.postDao(),
            appContext = appContext,
            watchlistRepository = watchlistRepository
        )
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepository(
            firestore = firebaseFirestore,
            auth = firebaseAuth,
            postDao = database.postDao(),
            storage = firebaseStorage
        )
    }

    val watchlistRepository: WatchlistRepository by lazy { WatchlistRepository(apiService = apiService) }
    val stockDetailsRepository: StockDetailsRepository by lazy {
        StockDetailsRepository(
            apiService = apiService,
            firestore = firebaseFirestore ?: FirebaseFirestore.getInstance(),
            auth = firebaseAuth
        )
    }
    val portfolioRepository: PortfolioRepository by lazy {
        PortfolioRepository(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance(),
            watchlistRepository = watchlistRepository
        )
    }

    val recentHotSearchStore: RecentHotSearchStore by lazy {
        RecentHotSearchStore(
            appContext.getSharedPreferences("hot_stocks_prefs", Context.MODE_PRIVATE)
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
            portfolioRepository = portfolioRepository,
            recentHotSearchStore = recentHotSearchStore
        )
    }
}

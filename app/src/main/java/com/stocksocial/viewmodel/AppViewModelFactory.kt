package com.stocksocial.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stocksocial.repository.ArticlesRepository
import com.stocksocial.repository.AuthRepository
import com.stocksocial.repository.FeedRepository
import com.stocksocial.repository.LocalPostsRepository
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.WatchlistRepository

class AppViewModelFactory(
    private val authRepository: AuthRepository? = null,
    private val feedRepository: FeedRepository? = null,
    private val profileRepository: ProfileRepository? = null,
    private val articlesRepository: ArticlesRepository? = null,
    private val watchlistRepository: WatchlistRepository? = null,
    private val localPostsRepository: LocalPostsRepository? = null
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository)
            }
            modelClass.isAssignableFrom(FeedViewModel::class.java) -> {
                FeedViewModel(feedRepository)
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(profileRepository)
            }
            modelClass.isAssignableFrom(ArticlesViewModel::class.java) -> {
                ArticlesViewModel(articlesRepository)
            }
            modelClass.isAssignableFrom(StocksViewModel::class.java) -> {
                StocksViewModel(watchlistRepository)
            }
            modelClass.isAssignableFrom(WatchlistViewModel::class.java) -> {
                WatchlistViewModel(watchlistRepository)
            }
            modelClass.isAssignableFrom(UserPostsViewModel::class.java) -> {
                UserPostsViewModel(localPostsRepository, authRepository)
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}

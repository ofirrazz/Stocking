package com.stocksocial.utils

import androidx.fragment.app.Fragment
import com.stocksocial.StockSocialApp
import com.stocksocial.viewmodel.AppViewModelFactory

val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as StockSocialApp).appContainer

val Fragment.appViewModelFactory: AppViewModelFactory
    get() = AppViewModelFactory(
        authRepository = appContainer.authRepository,
        feedRepository = appContainer.feedRepository,
        profileRepository = appContainer.profileRepository,
        articlesRepository = appContainer.articlesRepository,
        watchlistRepository = appContainer.watchlistRepository
    )

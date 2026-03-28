package com.stocksocial.utils

import androidx.fragment.app.Fragment
import com.stocksocial.StockSocialApp

val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as StockSocialApp).appContainer

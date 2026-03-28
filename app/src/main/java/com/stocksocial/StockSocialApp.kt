package com.stocksocial

import android.app.Application
import com.stocksocial.utils.AppContainer

class StockSocialApp : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}

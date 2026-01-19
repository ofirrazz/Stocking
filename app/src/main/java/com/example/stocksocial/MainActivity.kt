package com.example.stocksocial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.stocksocial.ui.navigation.AppNavHost
import com.example.stocksocial.ui.theme.StocksocialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    StocksocialTheme {
        val navController = rememberNavController()
        AppNavHost(navController)
    }
}

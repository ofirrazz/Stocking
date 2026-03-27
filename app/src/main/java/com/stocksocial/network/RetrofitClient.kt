package com.stocksocial.network

object RetrofitClient {

    // Backward-compatible accessor for old references.
    val apiService: ApiService by lazy {
        RetrofitInstance.create()
    }
}

package com.stocksocial.network

import com.stocksocial.BuildConfig
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("company-news")
    suspend fun getCompanyNews(
        @Query("symbol") symbol: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("token") token: String = BuildConfig.FINNHUB_TOKEN
    ): Response<List<FinnhubNewsItemDto>>

    @GET("quote")
    suspend fun getQuote(
        @Query("symbol") symbol: String,
        @Query("token") token: String = BuildConfig.FINNHUB_TOKEN
    ): Response<FinnhubQuoteDto>
}

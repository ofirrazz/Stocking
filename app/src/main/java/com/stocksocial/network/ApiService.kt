package com.stocksocial.network

import com.stocksocial.model.Article
import com.stocksocial.model.Post
import com.stocksocial.model.Stock
import com.stocksocial.model.User
import com.stocksocial.model.WatchlistItem
import com.stocksocial.model.auth.LoginRequest
import com.stocksocial.model.auth.LoginResponse
import com.stocksocial.model.auth.RegisterRequest
import com.stocksocial.model.auth.RegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("posts")
    suspend fun getFeedPosts(): Response<List<Post>>

    @GET("articles")
    suspend fun getArticles(): Response<List<Article>>

    @GET("stocks/search")
    suspend fun searchStocks(@Query("q") query: String): Response<List<Stock>>

    @GET("watchlist/{userId}")
    suspend fun getWatchlist(@Path("userId") userId: String): Response<List<WatchlistItem>>

    @GET("users/{userId}")
    suspend fun getUserProfile(@Path("userId") userId: String): Response<User>
}

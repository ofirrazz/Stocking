package com.stocksocial.network

import com.stocksocial.model.Article
import com.stocksocial.model.Post
import com.stocksocial.model.User
import com.stocksocial.model.WatchlistItem
import com.stocksocial.model.auth.AuthResponse
import com.stocksocial.model.auth.LoginRequest
import com.stocksocial.model.auth.RefreshTokenRequest
import com.stocksocial.model.auth.RefreshTokenResponse
import com.stocksocial.model.auth.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<RefreshTokenResponse>

    @GET("posts/feed")
    suspend fun getFeedPosts(): Response<List<Post>>

    @GET("users/me")
    suspend fun getMyProfile(): Response<User>

    @GET("users/me/posts")
    suspend fun getMyPosts(): Response<List<Post>>

    @GET("articles")
    suspend fun getArticles(): Response<List<Article>>

    @GET("articles/{id}")
    suspend fun getArticleById(
        @Path("id") articleId: String
    ): Response<Article>

    @GET("watchlist")
    suspend fun getWatchlist(): Response<List<WatchlistItem>>
}

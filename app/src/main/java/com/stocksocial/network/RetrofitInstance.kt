package com.stocksocial.network

import com.google.gson.GsonBuilder
import com.stocksocial.utils.Constants
import com.stocksocial.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    fun create(tokenManager: TokenManager? = null): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)

        if (tokenManager != null) {
            clientBuilder.addInterceptor(AuthInterceptor(tokenManager))
        }

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_API_URL)
            .client(clientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(ApiService::class.java)
    }
}

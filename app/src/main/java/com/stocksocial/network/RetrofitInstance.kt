package com.stocksocial.network

import com.google.gson.GsonBuilder
import com.stocksocial.BuildConfig
import com.stocksocial.utils.Constants
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    fun createApiService(): ApiService {
        val builder = OkHttpClient.Builder()
            .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Only attach HTTP logging on debug builds. Release builds must not leak the
        // Finnhub `token=` query parameter (or other request URLs) into Logcat.
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(loggingInterceptor)
        }

        return Retrofit.Builder()
            .baseUrl(Constants.BASE_API_URL)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(ApiService::class.java)
    }
}

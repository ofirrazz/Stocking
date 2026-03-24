package com.stocksocial.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        preferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun getToken(): String? = preferences.getString(KEY_JWT_TOKEN, null)

    fun clearToken() {
        preferences.edit().remove(KEY_JWT_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "stock_social_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
    }
}

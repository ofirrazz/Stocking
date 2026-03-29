package com.stocksocial.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        preferences.edit().putString(KEY_JWT_TOKEN, token).apply()
    }

    fun saveSession(token: String, userId: String, username: String) {
        preferences.edit()
            .putString(KEY_JWT_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getToken(): String? = preferences.getString(KEY_JWT_TOKEN, null)
    fun getUserId(): String? = preferences.getString(KEY_USER_ID, null)
    fun getUsername(): String? = preferences.getString(KEY_USERNAME, null)
    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    fun clearToken() {
        preferences.edit()
            .remove(KEY_JWT_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "stock_social_prefs"
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
    }
}

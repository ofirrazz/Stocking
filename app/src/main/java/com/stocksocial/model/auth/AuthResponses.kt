package com.stocksocial.model.auth

import com.stocksocial.model.User

data class LoginResponse(
    val token: String,
    val user: User
)

data class RegisterResponse(
    val token: String,
    val user: User
)

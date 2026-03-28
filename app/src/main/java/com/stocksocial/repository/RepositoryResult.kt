package com.stocksocial.repository

sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : RepositoryResult<Nothing>()
}

package com.stocksocial.model

data class SearchSuggestion(
    val title: String,
    val subtitle: String,
    val type: SearchSuggestionType,
    val userId: String? = null,
    val username: String? = null,
    val symbol: String? = null
)

enum class SearchSuggestionType {
    USER,
    STOCK
}

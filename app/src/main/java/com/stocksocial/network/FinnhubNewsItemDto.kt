package com.stocksocial.network

import com.google.gson.annotations.SerializedName

data class FinnhubNewsItemDto(
    val category: String?,
    val datetime: Long,
    val headline: String,
    val id: Long,
    val image: String?,
    val related: String?,
    val source: String,
    val summary: String,
    val url: String
)

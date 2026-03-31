package com.stocksocial.network

import com.google.gson.annotations.SerializedName

data class FinnhubSymbolSearchResponse(
    @SerializedName("count") val count: Int? = null,
    @SerializedName("result") val result: List<FinnhubSymbolSearchItem>? = null
)

data class FinnhubSymbolSearchItem(
    @SerializedName("description") val description: String? = null,
    @SerializedName("displaySymbol") val displaySymbol: String? = null,
    @SerializedName("symbol") val symbol: String? = null,
    @SerializedName("type") val type: String? = null
)

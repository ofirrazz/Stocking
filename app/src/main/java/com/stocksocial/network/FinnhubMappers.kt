package com.stocksocial.network

import com.stocksocial.model.Article
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun FinnhubNewsItemDto.toArticle(): Article {
    val idStr = "${id}_${datetime}"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val published = sdf.format(Date(datetime * 1000))
    return Article(
        id = idStr,
        category = category?.takeIf { it.isNotBlank() } ?: "news",
        title = headline,
        summary = summary,
        author = source,
        content = summary,
        source = source,
        imageUrl = image?.takeIf { it.isNotBlank() },
        publishedAt = published,
        url = url,
        localImagePath = null
    )
}

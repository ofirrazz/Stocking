package com.stocksocial.utils

/**
 * Pure-Kotlin helpers for parsing user-facing post content into structured ticker
 * information. Extracted from `FeedRepository` so it can be unit-tested without Firebase.
 */
object TickerParser {

    private val TICKER_REGEX = Regex("""\$(\p{Alpha}{1,6})\b""")

    /**
     * Returns the first uppercase ticker symbol mentioned in [content] (e.g. `$AAPL`),
     * or `null` if none is found.
     */
    fun extractTickerSymbol(content: String?): String? {
        if (content.isNullOrBlank()) return null
        return TICKER_REGEX.find(content)?.groupValues?.getOrNull(1)?.uppercase()
    }
}

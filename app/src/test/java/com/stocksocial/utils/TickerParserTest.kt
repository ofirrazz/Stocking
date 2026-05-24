package com.stocksocial.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure JVM tests for [TickerParser]. No Android or Firebase dependencies required.
 */
class TickerParserTest {

    @Test
    fun `returns null for blank content`() {
        assertNull(TickerParser.extractTickerSymbol(""))
        assertNull(TickerParser.extractTickerSymbol("   "))
        assertNull(TickerParser.extractTickerSymbol(null))
    }

    @Test
    fun `returns null when no dollar prefix is present`() {
        assertNull(TickerParser.extractTickerSymbol("AAPL is up today"))
        assertNull(TickerParser.extractTickerSymbol("Looking at Apple Inc."))
    }

    @Test
    fun `extracts simple ticker symbol`() {
        assertEquals("AAPL", TickerParser.extractTickerSymbol("Loving \$AAPL today"))
        assertEquals("TSLA", TickerParser.extractTickerSymbol("Sold my \$tsla"))
    }

    @Test
    fun `returns first ticker when multiple are mentioned`() {
        assertEquals(
            "NVDA",
            TickerParser.extractTickerSymbol("Comparing \$NVDA to \$AMD this quarter")
        )
    }

    @Test
    fun `respects max length of six characters`() {
        assertEquals("GOOGLE", TickerParser.extractTickerSymbol("\$GOOGLE has news"))
        // Seven letters after $ – regex stops at six but \b prevents partial match here,
        // so we expect null because GOOGLEZA is not a word boundary at position 6.
        assertNull(TickerParser.extractTickerSymbol("\$GOOGLEZA"))
    }

    @Test
    fun `uppercases extracted symbol`() {
        assertEquals("MSFT", TickerParser.extractTickerSymbol("hold \$msft"))
    }
}

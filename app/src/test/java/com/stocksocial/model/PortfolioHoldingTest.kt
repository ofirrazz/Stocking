package com.stocksocial.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioHoldingTest {

    @Test
    fun `investedValue equals shares times buyPrice`() {
        val h = PortfolioHolding(symbol = "AAPL", shares = 10.0, buyPrice = 150.0, currentPrice = 180.0)
        assertEquals(1500.0, h.investedValue, EPS)
    }

    @Test
    fun `currentValue equals shares times currentPrice`() {
        val h = PortfolioHolding(symbol = "NVDA", shares = 5.0, buyPrice = 200.0, currentPrice = 400.0)
        assertEquals(2000.0, h.currentValue, EPS)
    }

    @Test
    fun `pnlValue is positive when current price beats buy price`() {
        val h = PortfolioHolding(symbol = "AAPL", shares = 10.0, buyPrice = 150.0, currentPrice = 180.0)
        assertEquals(300.0, h.pnlValue, EPS)
    }

    @Test
    fun `pnlValue is negative when current price drops`() {
        val h = PortfolioHolding(symbol = "AAPL", shares = 10.0, buyPrice = 150.0, currentPrice = 120.0)
        assertEquals(-300.0, h.pnlValue, EPS)
    }

    @Test
    fun `pnlPercent returns zero when no investment`() {
        val h = PortfolioHolding(symbol = "X", shares = 0.0, buyPrice = 0.0, currentPrice = 10.0)
        assertEquals(0.0, h.pnlPercent, EPS)
    }

    @Test
    fun `pnlPercent calculates expected percentage gain`() {
        val h = PortfolioHolding(symbol = "AAPL", shares = 10.0, buyPrice = 100.0, currentPrice = 125.0)
        assertEquals(25.0, h.pnlPercent, EPS)
    }

    private companion object {
        const val EPS = 0.0001
    }
}

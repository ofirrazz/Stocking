package com.stocksocial.utils

import com.stocksocial.model.Article
import com.stocksocial.model.Post
import com.stocksocial.model.Stock
import com.stocksocial.model.StockSignal
import com.stocksocial.model.User

object DummyData {

    private val currentUser = User(
        id = "u1",
        username = "Ofir",
        email = "ofir@example.com",
        bio = "Long term investor and swing trader"
    )

    private val otherUsers = listOf(
        User(id = "u2", username = "Maya", email = "maya@example.com"),
        User(id = "u3", username = "Roi", email = "roi@example.com"),
        User(id = "u4", username = "Shani", email = "shani@example.com")
    )

    fun feedPosts(): List<Post> = listOf(
        Post(
            id = "p1",
            author = otherUsers[0],
            content = "Added NVDA on pullback. Watching 915 resistance.",
            createdAt = "Today",
            likesCount = 22,
            commentsCount = 5,
            stockSymbol = "NVDA",
            stockPrice = 912.60
        ),
        Post(
            id = "p2",
            author = otherUsers[1],
            content = "TSLA earnings week. Volatility might spike, size carefully.",
            createdAt = "2h ago",
            likesCount = 14,
            commentsCount = 3,
            stockSymbol = "TSLA",
            stockPrice = 188.30
        ),
        Post(
            id = "p3",
            author = currentUser,
            content = "Building a dividend watchlist for 2026.",
            createdAt = "Yesterday",
            likesCount = 31,
            commentsCount = 11,
            stockSymbol = "AAPL",
            stockPrice = 227.45
        )
    )

    fun userPosts(): List<Post> = listOf(
        Post(
            id = "up1",
            author = currentUser,
            content = "Took partial profit on AAPL after breakout.",
            createdAt = "Today",
            likesCount = 10,
            commentsCount = 2,
            stockSymbol = "AAPL",
            stockPrice = 227.45,
            imageUrl = "mock://chart-image"
        ),
        Post(
            id = "up2",
            author = currentUser,
            content = "My weekly portfolio review is done. Risk is under control.",
            createdAt = "3 days ago",
            likesCount = 18,
            commentsCount = 4,
            stockSymbol = "MSFT",
            stockPrice = 431.90,
            videoUrl = "mock://trade-recap-video"
        ),
        Post(
            id = "up3",
            author = currentUser,
            content = "Uploaded both chart snapshot and recap video for this swing setup.",
            createdAt = "5 days ago",
            likesCount = 27,
            commentsCount = 9,
            stockSymbol = "NVDA",
            stockPrice = 912.60,
            imageUrl = "mock://setup-image",
            videoUrl = "mock://setup-video"
        )
    )

    fun articles(): List<Article> = listOf(
        Article(
            id = "a1",
            title = "Fed Signals Potential Rate Pause",
            summary = "Markets react as inflation data cools for a second month.",
            source = "Bloomberg"
        ),
        Article(
            id = "a2",
            title = "AI Chip Demand Lifts Semiconductor Sector",
            summary = "Analysts revise growth forecasts for the next two quarters.",
            source = "Reuters"
        ),
        Article(
            id = "a3",
            title = "Energy Stocks Rebound on Oil Strength",
            summary = "Crude climbs above key level and supports energy names.",
            source = "WSJ"
        )
    )

    fun watchlistStocks(): List<Stock> = listOf(
        Stock(symbol = "AAPL", name = "Apple", price = 227.45, dailyChangePercent = 1.14),
        Stock(symbol = "NVDA", name = "NVIDIA", price = 912.60, dailyChangePercent = 2.48),
        Stock(symbol = "MSFT", name = "Microsoft", price = 431.90, dailyChangePercent = 0.67),
        Stock(symbol = "TSLA", name = "Tesla", price = 188.30, dailyChangePercent = -1.21)
    )

    fun marketIndices(): List<Stock> = listOf(
        Stock(symbol = "SPY", name = "S&P 500 ETF", price = 523.81, dailyChangePercent = 0.52),
        Stock(symbol = "DIA", name = "Dow Jones ETF", price = 392.44, dailyChangePercent = -0.17),
        Stock(symbol = "QQQ", name = "Nasdaq 100 ETF", price = 449.26, dailyChangePercent = 0.93)
    )

    fun trendingStocks(): List<Stock> = listOf(
        Stock(symbol = "AAPL", name = "Apple", price = 227.45, dailyChangePercent = 1.14),
        Stock(symbol = "MSFT", name = "Microsoft", price = 431.90, dailyChangePercent = 0.67),
        Stock(symbol = "NVDA", name = "NVIDIA", price = 912.60, dailyChangePercent = 2.48),
        Stock(symbol = "AMD", name = "Advanced Micro Devices", price = 176.23, dailyChangePercent = 1.92),
        Stock(symbol = "TSLA", name = "Tesla", price = 188.30, dailyChangePercent = -1.21)
    )

    fun topSignals(): List<StockSignal> = listOf(
        StockSignal(
            symbol = "AAPL",
            ideaTitle = "Breakout above consolidation range",
            confidence = "High",
            currentPrice = 227.45,
            targetPrice = 239.00,
            trend = "Bullish"
        ),
        StockSignal(
            symbol = "MSFT",
            ideaTitle = "Support bounce with volume confirmation",
            confidence = "Medium",
            currentPrice = 431.90,
            targetPrice = 446.50,
            trend = "Bullish"
        ),
        StockSignal(
            symbol = "TSLA",
            ideaTitle = "Potential downside if 185 fails",
            confidence = "Medium",
            currentPrice = 188.30,
            targetPrice = 176.00,
            trend = "Bearish"
        )
    )
}

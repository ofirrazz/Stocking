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
            category = "Economics",
            title = "Fed Signals Potential Rate Pause",
            summary = "Markets react as inflation data cools for a second month.",
            author = "Elena Morris",
            content = "Federal Reserve officials indicated that the policy path may become less restrictive if inflation continues to cool. Analysts expect bond yields to remain sensitive to upcoming labor and CPI prints. Equity markets moved higher as growth sectors outperformed.",
            source = "Bloomberg"
            ,
            publishedAt = "45m ago"
        ),
        Article(
            id = "a2",
            category = "Technology",
            title = "AI Chip Demand Lifts Semiconductor Sector",
            summary = "Analysts revise growth forecasts for the next two quarters.",
            author = "Jacob Lee",
            content = "Institutional desks continue to rotate into semiconductor names after strong guidance updates from major suppliers. Capacity expansion remains a key theme, while valuation sensitivity has increased as expectations rise.",
            source = "Reuters"
            ,
            publishedAt = "1h ago"
        ),
        Article(
            id = "a3",
            category = "Automotive",
            title = "Energy Stocks Rebound on Oil Strength",
            summary = "Crude climbs above key level and supports energy names.",
            author = "Nora Patel",
            content = "Energy names rebounded as crude prices reclaimed an important technical level. Traders are watching supply commentary and refinery utilization data for confirmation of trend continuation.",
            source = "WSJ"
            ,
            publishedAt = "3h ago"
        ),
        Article(
            id = "a4",
            category = "Earnings",
            title = "AAPL Beats Revenue Expectations in Services",
            summary = "Services growth offsets softer hardware demand in select regions.",
            author = "Michael Grant",
            content = "Apple reported stronger-than-expected services revenue, helping overall margins despite mixed device demand. Management highlighted recurring subscription momentum and disciplined cost controls.",
            source = "CNBC",
            publishedAt = "5h ago"
        ),
        Article(
            id = "a5",
            category = "Earnings",
            title = "MSFT Cloud Growth Accelerates for Third Straight Quarter",
            summary = "Azure momentum supports improved forward guidance.",
            author = "Dana Cohen",
            content = "Microsoft posted another solid quarter for cloud infrastructure and enterprise productivity. Improved utilization from large accounts helped operating leverage in the cloud segment.",
            source = "The Information",
            publishedAt = "Yesterday"
        ),
        Article(
            id = "a6",
            category = "Economics",
            title = "US Jobless Claims Tick Up, Labor Market Still Tight",
            summary = "Weekly claims rise modestly but remain below long-term averages.",
            author = "Rachel Kim",
            content = "Initial jobless claims rose slightly this week, but economists note that broader labor data still points to resilient hiring conditions. Markets now await payroll revisions and wage growth trends.",
            source = "Financial Times",
            publishedAt = "Yesterday"
        )
    )

    fun articleById(articleId: String): Article? = articles().firstOrNull { it.id == articleId }

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
        Stock(symbol = "NVDA", name = "NVIDIA", price = 912.60, dailyChangePercent = 5.67),
        Stock(symbol = "AAPL", name = "Apple", price = 227.45, dailyChangePercent = 2.10),
        Stock(symbol = "TSLA", name = "Tesla", price = 188.30, dailyChangePercent = -1.23),
        Stock(symbol = "MSFT", name = "Microsoft", price = 431.90, dailyChangePercent = 1.90),
        Stock(symbol = "GOOGL", name = "Alphabet", price = 175.20, dailyChangePercent = -0.50)
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

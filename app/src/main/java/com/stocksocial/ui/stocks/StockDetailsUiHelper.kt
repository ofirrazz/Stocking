package com.stocksocial.ui.stocks

import com.stocksocial.R
import com.stocksocial.model.AnalystRecommendation
import com.stocksocial.model.Stock
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class ScoreRowUi(
    val labelRes: Int,
    val iconTintIsGreen: Boolean,
    val score: Float,
    val progress: Int
)

data class StockDerivedMetrics(
    val peText: String,
    val marketCapText: String,
    val dividendText: String,
    val epsText: String,
    val overallScore: Float,
    val ratingLabelRes: Int,
    val growth: Float,
    val valueScore: Float,
    val momentum: Float,
    val quality: Float,
    val sentimentPercent: Int,
    val scoreRows: List<ScoreRowUi>
)

object StockDetailsUiHelper {

    fun buildDerived(stock: Stock, latestRec: AnalystRecommendation?): StockDerivedMetrics {
        val score = overallScore(latestRec, stock.dailyChangePercent)
        val g = clamp(score + (stock.dailyChangePercent * 0.08f).toFloat())
        val v = clamp(score - 0.4f + (abs(stock.symbol.hashCode() % 5) * 0.1f))
        val m = clamp(score + 0.2f)
        val q = clamp(score + 0.5f)
        val sentiment = sentimentPercent(latestRec)

        val rows = listOf(
            ScoreRowUi(R.string.stock_score_growth, true, g, (g * 10f).roundToInt().coerceIn(0, 100)),
            ScoreRowUi(R.string.stock_score_value, false, v, (v * 10f).roundToInt().coerceIn(0, 100)),
            ScoreRowUi(R.string.stock_score_momentum, false, m, (m * 10f).roundToInt().coerceIn(0, 100)),
            ScoreRowUi(R.string.stock_score_quality, true, q, (q * 10f).roundToInt().coerceIn(0, 100))
        )

        val seed = abs(stock.symbol.hashCode() % 1000) / 1000.0
        val pe = 12.0 + seed * 40
        val eps = stock.price / pe * (0.85 + seed * 0.3)
        val div = 0.2 + seed * 1.4
        val capBillions = 800.0 + seed * 2200

        return StockDerivedMetrics(
            peText = String.format(Locale.US, "%.1f", pe),
            marketCapText = formatCap(capBillions),
            dividendText = String.format(Locale.US, "%.2f%%", div),
            epsText = String.format(Locale.US, "$%.2f", eps),
            overallScore = score,
            ratingLabelRes = ratingLabel(score),
            growth = g,
            valueScore = v,
            momentum = m,
            quality = q,
            sentimentPercent = sentiment,
            scoreRows = rows
        )
    }

    private fun formatCap(billions: Double): String {
        return when {
            billions >= 1000 -> String.format(Locale.US, "$%.2fT", billions / 1000)
            else -> String.format(Locale.US, "$%.2fB", billions)
        }
    }

    private fun clamp(x: Float): Float = x.coerceIn(1f, 9.9f)

    private fun overallScore(rec: AnalystRecommendation?, dailyPct: Double): Float {
        if (rec == null) {
            return clamp(7.2f + (dailyPct * 0.05f).toFloat())
        }
        val bull = rec.strongBuy + rec.buy
        val bear = rec.strongSell + rec.sell
        val total = (bull + bear + rec.hold).coerceAtLeast(1)
        val bias = (bull - bear).toFloat() / total.toFloat()
        return clamp(6.5f + bias * 2.2f + (dailyPct * 0.04f).toFloat())
    }

    private fun sentimentPercent(rec: AnalystRecommendation?): Int {
        if (rec == null) return 62
        val bull = rec.strongBuy + rec.buy
        val bear = rec.strongSell + rec.sell
        val t = bull + bear + rec.hold
        if (t <= 0) return 55
        return ((bull * 100.0 / t).roundToInt()).coerceIn(35, 92)
    }

    private fun ratingLabel(score: Float): Int = when {
        score >= 8.5f -> R.string.stock_rating_excellent
        score >= 7.2f -> R.string.stock_rating_good
        score >= 5.8f -> R.string.stock_rating_ok
        else -> R.string.stock_rating_weak
    }
}

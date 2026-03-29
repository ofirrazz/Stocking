package com.stocksocial.ui.adapters

import android.content.Context
import androidx.annotation.ColorInt
import com.stocksocial.R
import java.util.Locale

object StockUiFormatter {
    fun formatPrice(price: Double): String = "$${"%.2f".format(Locale.US, price)}"

    fun formatChangePercent(changePercent: Double): String {
        val sign = if (changePercent >= 0) "+" else ""
        return "$sign${"%.2f".format(Locale.US, changePercent)}%"
    }

    @ColorInt
    fun resolveChangeColor(context: Context, changePercent: Double): Int {
        val colorRes = if (changePercent >= 0) R.color.feed_accent_green else R.color.feed_accent_red
        return context.getColor(colorRes)
    }
}

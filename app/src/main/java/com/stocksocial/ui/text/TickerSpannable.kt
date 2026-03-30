package com.stocksocial.ui.text

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.stocksocial.R
import java.util.regex.Pattern

object TickerSpannable {
    private val pattern = Pattern.compile("\\\$[A-Za-z]{1,5}")

    fun format(context: Context, text: String): CharSequence {
        val gold = context.getColor(R.color.primary_gold)
        val spannable = SpannableString(text)
        val m = pattern.matcher(text)
        while (m.find()) {
            spannable.setSpan(ForegroundColorSpan(gold), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }
}

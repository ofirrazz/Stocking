package com.stocksocial.data.prefs

import android.content.SharedPreferences

class RecentHotSearchStore(private val prefs: SharedPreferences) {

    fun getSymbolsOrdered(): List<String> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val out = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        for (part in raw.split("|")) {
            val s = part.trim().uppercase()
            if (s.isNotEmpty() && s !in seen) {
                seen.add(s)
                out.add(s)
            }
        }
        return out
    }

    fun prependSymbol(symbol: String) {
        val sym = symbol.trim().uppercase()
        if (sym.isEmpty()) return
        val cur = getSymbolsOrdered().toMutableList()
        cur.remove(sym)
        cur.add(0, sym)
        while (cur.size > MAX) cur.removeAt(cur.lastIndex)
        prefs.edit().putString(KEY, cur.joinToString("|")).apply()
    }

    companion object {
        private const val KEY = "recent_hot_symbols"
        private const val MAX = 20
    }
}

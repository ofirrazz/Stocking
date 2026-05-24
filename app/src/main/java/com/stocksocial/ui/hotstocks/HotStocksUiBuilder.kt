package com.stocksocial.ui.hotstocks

import androidx.annotation.StringRes
import com.stocksocial.R
import com.stocksocial.model.FeedHotStockCategory
import com.stocksocial.model.Stock
import com.stocksocial.viewmodel.StocksUiData

sealed class HotStocksGroupedRow {
    data class Header(@StringRes val titleRes: Int) : HotStocksGroupedRow()
    data class StockRow(
        val stock: Stock,
        val rankInSection: Int,
        val isFavorite: Boolean
    ) : HotStocksGroupedRow()
}

object HotStocksUiBuilder {

    fun build(data: StocksUiData, filter: String): List<HotStocksGroupedRow> {
        val q = filter.trim()
        if (q.isNotEmpty()) {
            val local = collectUniverse(data).filter { matches(it, q) }
            val remote = data.hotSearchResults.filter { matches(it, q) }
            val merged = LinkedHashMap<String, Stock>()
            remote.forEach { merged[it.symbol.uppercase()] = it }
            local.forEach { sym -> merged.putIfAbsent(sym.symbol.uppercase(), sym) }
            if (merged.isEmpty()) return emptyList()
            return section(R.string.hot_search_results, merged.values.toList(), data.favoriteSymbols)
        }

        val seen = mutableSetOf<String>()
        val out = mutableListOf<HotStocksGroupedRow>()

        appendSection(
            out, R.string.hot_section_favorites,
            data.favoriteStocks.filter { takeIfNew(it, seen) },
            data.favoriteSymbols
        )
        appendSection(
            out, R.string.hot_section_recent,
            data.recentSearches.filter { takeIfNew(it, seen) },
            data.favoriteSymbols
        )
        appendSection(
            out, R.string.hot_section_trending,
            stocksInPreferredOrder(data.trendingStocks, FeedHotStockCategory.defaultTrendingOrder, 5)
                .filter { takeIfNew(it, seen) },
            data.favoriteSymbols
        )
        appendSection(
            out, R.string.chip_tech,
            stocksInPreferredOrder(data.hotTechStocks, FeedHotStockCategory.technology)
                .filter { takeIfNew(it, seen) },
            data.favoriteSymbols
        )
        appendSection(
            out, R.string.chip_banking,
            stocksInPreferredOrder(data.hotBankingStocks, FeedHotStockCategory.banking)
                .filter { takeIfNew(it, seen) },
            data.favoriteSymbols
        )
        appendSection(
            out, R.string.chip_crypto,
            stocksInPreferredOrder(data.hotCryptoStocks, FeedHotStockCategory.crypto)
                .filter { takeIfNew(it, seen) },
            data.favoriteSymbols
        )

        return out
    }

    private fun collectUniverse(data: StocksUiData): List<Stock> {
        val bySym = LinkedHashMap<String, Stock>()
        fun addAll(list: List<Stock>) {
            list.forEach { s ->
                bySym.putIfAbsent(s.symbol.uppercase(), s)
            }
        }
        addAll(data.favoriteStocks)
        addAll(data.recentSearches)
        addAll(stocksInPreferredOrder(data.trendingStocks, FeedHotStockCategory.defaultTrendingOrder, 5))
        addAll(data.hotTechStocks)
        addAll(data.hotBankingStocks)
        addAll(data.hotCryptoStocks)
        return bySym.values.toList()
    }

    private fun matches(stock: Stock, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        if (stock.symbol.lowercase().contains(q)) return true
        val name = stock.name.trim()
        return name.isNotEmpty() && name.lowercase().contains(q)
    }

    private fun takeIfNew(stock: Stock, seen: MutableSet<String>): Boolean {
        val u = stock.symbol.uppercase()
        if (u in seen) return false
        seen.add(u)
        return true
    }

    private fun stocksInPreferredOrder(
        list: List<Stock>,
        preferredOrder: List<String>,
        maxSlots: Int = Int.MAX_VALUE
    ): List<Stock> {
        if (list.isEmpty()) return emptyList()
        val items = mutableListOf<Stock>()
        if (preferredOrder.isEmpty()) {
            return list.take(maxSlots.coerceAtMost(list.size))
        }
        val bySymbol = list.associateBy { it.symbol.uppercase() }
        for (sym in preferredOrder) {
            if (items.size >= maxSlots) break
            bySymbol[sym.uppercase()]?.let { items.add(it) }
        }
        val remaining = (maxSlots - items.size).coerceAtLeast(0)
        if (remaining > 0) {
            val used = items.map { it.symbol.uppercase() }.toSet()
            list.filter { it.symbol.uppercase() !in used }.take(remaining).forEach { items.add(it) }
        }
        return items
    }

    private fun section(
        @StringRes titleRes: Int,
        stocks: List<Stock>,
        favoriteSymbols: Set<String>
    ): List<HotStocksGroupedRow> {
        val out = mutableListOf<HotStocksGroupedRow>()
        appendSection(out, titleRes, stocks, favoriteSymbols)
        return out
    }

    private fun appendSection(
        out: MutableList<HotStocksGroupedRow>,
        @StringRes titleRes: Int,
        stocks: List<Stock>,
        favoriteSymbols: Set<String>
    ) {
        if (stocks.isEmpty()) return
        out.add(HotStocksGroupedRow.Header(titleRes))
        stocks.forEachIndexed { index, stock ->
            val sym = stock.symbol.uppercase()
            out.add(
                HotStocksGroupedRow.StockRow(
                    stock = stock,
                    rankInSection = index + 1,
                    isFavorite = sym in favoriteSymbols
                )
            )
        }
    }
}

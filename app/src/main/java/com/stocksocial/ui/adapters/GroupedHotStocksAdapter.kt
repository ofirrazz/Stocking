package com.stocksocial.ui.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.R
import com.stocksocial.databinding.ItemFeedHotStockBinding
import com.stocksocial.databinding.ItemHotStocksHeaderBinding
import com.stocksocial.model.Stock
import com.stocksocial.ui.hotstocks.HotStocksGroupedRow
import java.util.Locale

class GroupedHotStocksAdapter(
    private val onStockClick: (String) -> Unit,
    private val onFavoriteClick: (Stock) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<HotStocksGroupedRow>()

    fun submit(list: List<HotStocksGroupedRow>) {
        rows.clear()
        rows.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is HotStocksGroupedRow.Header -> VIEW_HEADER
        is HotStocksGroupedRow.StockRow -> VIEW_STOCK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_HEADER -> HeaderVH(
                ItemHotStocksHeaderBinding.inflate(inflater, parent, false)
            )
            VIEW_STOCK -> StockVH(
                ItemFeedHotStockBinding.inflate(inflater, parent, false)
            )
            else -> error("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is HotStocksGroupedRow.Header -> (holder as HeaderVH).bind(row.titleRes)
            is HotStocksGroupedRow.StockRow -> (holder as StockVH).bind(row)
        }
    }

    override fun getItemCount(): Int = rows.size

    private class HeaderVH(
        private val binding: ItemHotStocksHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(titleRes: Int) {
            binding.sectionTitleText.setText(titleRes)
        }
    }

    private inner class StockVH(
        private val binding: ItemFeedHotStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: HotStocksGroupedRow.StockRow) {
            val stock = row.stock
            val ctx = binding.root.context
            binding.symbolText.text = displayTitle(stock)
            val mentions = 800 + kotlin.math.abs(stock.symbol.hashCode() % 2200)
            binding.mentionsText.text = ctx.getString(
                R.string.mentions_format,
                java.text.NumberFormat.getIntegerInstance(Locale.US).format(mentions)
            )
            val rank = row.rankInSection
            binding.rankBadge.text = rank.toString()
            val bg = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            val fill = when (rank) {
                1 -> ContextCompat.getColor(ctx, R.color.rank_gold_bright)
                2 -> ContextCompat.getColor(ctx, R.color.rank_gold_dim)
                3 -> ContextCompat.getColor(ctx, R.color.rank_gray)
                else -> ContextCompat.getColor(ctx, R.color.rank_gray_dark)
            }
            bg.setColor(fill)
            binding.rankBadge.background = bg
            binding.rankBadge.setTextColor(
                if (rank <= 2) ContextCompat.getColor(ctx, R.color.black)
                else ContextCompat.getColor(ctx, R.color.text_primary)
            )
            val pct = stock.dailyChangePercent
            val sign = if (pct >= 0) "+" else ""
            binding.changeText.text = "$sign${String.format(Locale.US, "%.2f", pct)}%"
            val green = ContextCompat.getColor(ctx, R.color.success_green)
            val red = ContextCompat.getColor(ctx, R.color.destructive_red)
            if (pct >= 0) {
                binding.changeText.setTextColor(green)
                binding.trendIcon.setImageResource(R.drawable.ic_trend_up)
            } else {
                binding.changeText.setTextColor(red)
                binding.trendIcon.setImageResource(R.drawable.ic_trend_down)
            }
            val muted = ContextCompat.getColor(ctx, R.color.text_muted)
            if (row.isFavorite) {
                binding.favoriteStarButton.setImageResource(android.R.drawable.btn_star_big_on)
                binding.favoriteStarButton.setColorFilter(ContextCompat.getColor(ctx, R.color.primary_gold))
            } else {
                binding.favoriteStarButton.setImageResource(android.R.drawable.btn_star_big_off)
                binding.favoriteStarButton.setColorFilter(muted)
            }
            binding.favoriteStarButton.setOnClickListener {
                onFavoriteClick(stock)
            }
            binding.root.setOnClickListener { onStockClick(stock.symbol) }
        }
    }

    private fun displayTitle(stock: Stock): String {
        val name = stock.name.trim()
        if (name.isNotEmpty() && !name.equals(stock.symbol, ignoreCase = true)) {
            return name
        }
        val sym = stock.symbol
        return if (sym.contains(':')) {
            val pair = sym.substringAfter(':')
            val base = pair
                .removeSuffix("USDT")
                .removeSuffix("USD")
                .removeSuffix("BUSD")
            if (base.isNotEmpty()) "$$base" else "$$sym"
        } else {
            "$$sym"
        }
    }

    companion object {
        private const val VIEW_HEADER = 0
        private const val VIEW_STOCK = 1
    }
}

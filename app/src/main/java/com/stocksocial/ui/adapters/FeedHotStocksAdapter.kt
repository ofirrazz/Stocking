package com.stocksocial.ui.adapters

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.R
import com.stocksocial.databinding.ItemFeedHotStockBinding
import com.stocksocial.model.Stock
import java.util.Locale

class FeedHotStocksAdapter(
    private val onStockClick: (String) -> Unit
) : RecyclerView.Adapter<FeedHotStocksAdapter.VH>() {

    private val items = mutableListOf<Stock>()

    fun submitOrdered(list: List<Stock>) {
        items.clear()
        val maxSlots = 5
        val order = listOf("NVDA", "AAPL", "TSLA", "MSFT", "GOOGL", "AMD")
        val inOrder = order.toSet()
        val bySymbol = list.associateBy { it.symbol }
        for (sym in order) {
            if (items.size >= maxSlots) break
            bySymbol[sym]?.let { items.add(it) }
        }
        val remaining = (maxSlots - items.size).coerceAtLeast(0)
        if (remaining > 0) {
            list.filter { it.symbol !in inOrder }.take(remaining).forEach { items.add(it) }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFeedHotStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val stock = items[position]
        val rank = position + 1
        val ctx = holder.binding.root.context
        holder.binding.symbolText.text = "$${stock.symbol}"
        val mentions = 800 + kotlin.math.abs(stock.symbol.hashCode() % 2200)
        holder.binding.mentionsText.text = ctx.getString(
            R.string.mentions_format,
            java.text.NumberFormat.getIntegerInstance(Locale.US).format(mentions)
        )
        holder.binding.rankBadge.text = rank.toString()
        val bg = GradientDrawable().apply { shape = GradientDrawable.OVAL }
        val fill = when (rank) {
            1 -> ContextCompat.getColor(ctx, R.color.rank_gold_bright)
            2 -> ContextCompat.getColor(ctx, R.color.rank_gold_dim)
            3 -> ContextCompat.getColor(ctx, R.color.rank_gray)
            else -> ContextCompat.getColor(ctx, R.color.rank_gray_dark)
        }
        bg.setColor(fill)
        holder.binding.rankBadge.background = bg
        holder.binding.rankBadge.setTextColor(
            if (rank <= 2) ContextCompat.getColor(ctx, R.color.black)
            else ContextCompat.getColor(ctx, R.color.text_primary)
        )
        val pct = stock.dailyChangePercent
        val sign = if (pct >= 0) "+" else ""
        holder.binding.changeText.text = "$sign${String.format(Locale.US, "%.2f", pct)}%"
        val green = ContextCompat.getColor(ctx, R.color.success_green)
        val red = ContextCompat.getColor(ctx, R.color.destructive_red)
        if (pct >= 0) {
            holder.binding.changeText.setTextColor(green)
            holder.binding.trendIcon.setImageResource(R.drawable.ic_trend_up)
            holder.binding.trendIcon.clearColorFilter()
        } else {
            holder.binding.changeText.setTextColor(red)
            holder.binding.trendIcon.setImageResource(R.drawable.ic_trend_down)
            holder.binding.trendIcon.clearColorFilter()
        }
        holder.binding.root.setOnClickListener { onStockClick(stock.symbol) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemFeedHotStockBinding) : RecyclerView.ViewHolder(binding.root)
}

package com.stocksocial.ui.portfolio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.R
import com.stocksocial.databinding.ItemHoldingBinding
import java.util.Locale
import kotlin.math.abs

data class HoldingUi(
    val ticker: String,
    val name: String,
    val shares: Int,
    val avgPrice: Float,
    val currentPrice: Float,
    val changePercent: Float
)

class HoldingsAdapter : ListAdapter<HoldingUi, HoldingsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHoldingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemHoldingBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HoldingUi) {
            val ctx = binding.root.context
            val gainLoss = item.shares * (item.currentPrice - item.avgPrice)
            val positive = gainLoss >= 0
            val trendId =
                if (positive) R.drawable.ic_trending_up_24 else R.drawable.ic_trending_down_24
            val colorId = if (positive) R.color.success else R.color.destructive
            val color = ContextCompat.getColor(ctx, colorId)

            binding.tickerText.text = "$" + item.ticker
            binding.sharesText.text =
                ctx.getString(R.string.shares_count, item.shares)
            binding.priceText.text = String.format(Locale.US, "$%.2f", item.currentPrice)
            binding.gainLossText.text = String.format(
                Locale.US,
                if (positive) "+$%,.2f" else "-$%,.2f",
                abs(gainLoss)
            )
            binding.gainLossText.setTextColor(color)
            binding.changePercentText.text = String.format(
                Locale.US,
                if (positive) "+%.2f%%" else "%.2f%%",
                item.changePercent
            )
            binding.changePercentText.setTextColor(color)
            binding.trendIcon.setImageResource(trendId)
            binding.trendIcon.setColorFilter(color)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HoldingUi>() {
            override fun areItemsTheSame(a: HoldingUi, b: HoldingUi) = a.ticker == b.ticker
            override fun areContentsTheSame(a: HoldingUi, b: HoldingUi) = a == b
        }
    }
}

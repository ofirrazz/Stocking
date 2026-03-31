package com.stocksocial.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.ItemPortfolioHoldingBinding
import com.stocksocial.model.PortfolioHolding
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

class PortfolioHoldingsAdapter(
    private val onRowClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<PortfolioHoldingsAdapter.VH>() {
    private val items = mutableListOf<PortfolioHolding>()
    private val currency = NumberFormat.getCurrencyInstance(Locale.US)

    fun submitList(data: List<PortfolioHolding>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPortfolioHoldingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx = holder.binding.root.context
        holder.binding.symbolText.text = item.symbol
        holder.binding.companyNameText.text = companyLabel(item)
        holder.binding.positionValueText.text = currency.format(item.currentValue)
        holder.binding.sharesSubtitleText.text = formatShares(ctx, item.shares)
        holder.binding.avgStatText.text =
            ctx.getString(R.string.portfolio_avg) + " " + currency.format(item.buyPrice)
        holder.binding.currentStatText.text =
            ctx.getString(R.string.portfolio_current) + " " + currency.format(item.currentPrice)
        val pct = item.pnlPercent
        val sign = if (pct >= 0) "+" else ""
        holder.binding.perfPercentText.text = "$sign${String.format(Locale.US, "%.2f", pct)}%"
        val color = StockUiFormatter.resolveChangeColor(ctx, pct)
        holder.binding.perfPercentText.setTextColor(color)
        if (pct >= 0) {
            holder.binding.perfTrendIcon.setImageResource(android.R.drawable.arrow_up_float)
        } else {
            holder.binding.perfTrendIcon.setImageResource(android.R.drawable.arrow_down_float)
        }
        holder.binding.perfTrendIcon.imageTintList = ColorStateList.valueOf(color)

        val logoUrl = "https://finnhub.io/api/logo?symbol=${item.symbol}"
        Glide.with(holder.binding.stockLogo)
            .load(logoUrl)
            .circleCrop()
            .placeholder(R.drawable.bg_notification_stock_avatar)
            .error(R.drawable.ic_notification_dollar)
            .into(holder.binding.stockLogo)
        holder.binding.root.setOnClickListener {
            onRowClick?.invoke(item.symbol)
        }
    }

    private fun formatShares(ctx: android.content.Context, shares: Double): String {
        val n = if (abs(shares - shares.toLong().toDouble()) < 1e-4) {
            shares.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", shares)
        }
        return ctx.getString(R.string.portfolio_shares_line, n)
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemPortfolioHoldingBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val knownNames = mapOf(
            "AAPL" to "Apple Inc.",
            "NVDA" to "NVIDIA Corp.",
            "MSFT" to "Microsoft Corp.",
            "GOOGL" to "Alphabet Inc.",
            "TSLA" to "Tesla Inc.",
            "AMZN" to "Amazon.com Inc.",
            "META" to "Meta Platforms Inc.",
            "JPM" to "JPMorgan Chase & Co."
        )

        fun companyLabel(item: PortfolioHolding): String {
            item.displayName?.takeIf { it.isNotBlank() }?.let { return it }
            val sym = item.symbol.uppercase(Locale.US)
            return knownNames[sym] ?: sym
        }
    }
}

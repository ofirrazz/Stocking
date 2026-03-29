package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemTopSignalBinding
import com.stocksocial.model.StockSignal

class TopSignalsAdapter : RecyclerView.Adapter<TopSignalsAdapter.TopSignalViewHolder>() {

    private val items = mutableListOf<StockSignal>()

    fun submitList(list: List<StockSignal>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopSignalViewHolder {
        val binding = ItemTopSignalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TopSignalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopSignalViewHolder, position: Int) {
        val item = items[position]
        holder.binding.symbolText.text = item.symbol
        holder.binding.ideaTitleText.text = item.ideaTitle
        holder.binding.confidenceText.text = item.confidence
        holder.binding.priceRangeText.text =
            "$${"%.2f".format(item.currentPrice)} -> $${"%.2f".format(item.targetPrice)}"
        holder.binding.trendText.text = item.trend
        holder.binding.trendText.setTextColor(
            holder.binding.root.context.getColor(
                if (item.trend == "Bullish") com.stocksocial.R.color.feed_accent_green
                else com.stocksocial.R.color.feed_accent_red
            )
        )
    }

    override fun getItemCount(): Int = items.size

    class TopSignalViewHolder(val binding: ItemTopSignalBinding) : RecyclerView.ViewHolder(binding.root)
}

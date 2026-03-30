package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemMarketIndexBinding
import com.stocksocial.model.Stock

class MarketIndexAdapter : RecyclerView.Adapter<MarketIndexAdapter.MarketIndexViewHolder>() {

    private val items = mutableListOf<Stock>()

    fun submitList(list: List<Stock>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketIndexViewHolder {
        val binding = ItemMarketIndexBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MarketIndexViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarketIndexViewHolder, position: Int) {
        val item = items[position]
        holder.binding.symbolText.text = item.symbol
        holder.binding.priceText.text = StockUiFormatter.formatPrice(item.price)
        holder.binding.changeText.text = StockUiFormatter.formatChangePercent(item.dailyChangePercent)
        holder.binding.changeText.setTextColor(
            StockUiFormatter.resolveChangeColor(holder.binding.root.context, item.dailyChangePercent)
        )
    }

    override fun getItemCount(): Int = items.size

    class MarketIndexViewHolder(val binding: ItemMarketIndexBinding) : RecyclerView.ViewHolder(binding.root)
}

package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemTrendingStockBinding
import com.stocksocial.model.Stock

class TrendingStocksAdapter(
    private val onStockClick: (Stock) -> Unit
) : RecyclerView.Adapter<TrendingStocksAdapter.TrendingStockViewHolder>() {

    private val items = mutableListOf<Stock>()

    fun submitList(list: List<Stock>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendingStockViewHolder {
        val binding = ItemTrendingStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrendingStockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrendingStockViewHolder, position: Int) {
        val item = items[position]
        holder.binding.symbolText.text = item.symbol
        holder.binding.nameText.text = item.name
        holder.binding.priceText.text = StockUiFormatter.formatPrice(item.price)
        holder.binding.changeText.text = StockUiFormatter.formatChangePercent(item.dailyChangePercent)
        holder.binding.changeText.setTextColor(
            StockUiFormatter.resolveChangeColor(holder.binding.root.context, item.dailyChangePercent)
        )
        holder.binding.root.setOnClickListener { onStockClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class TrendingStockViewHolder(val binding: ItemTrendingStockBinding) : RecyclerView.ViewHolder(binding.root)
}

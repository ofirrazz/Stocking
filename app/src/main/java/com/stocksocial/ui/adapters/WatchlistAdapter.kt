package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemStockBinding
import com.stocksocial.model.Stock

class WatchlistAdapter(
    private val items: List<Stock> = emptyList()
) : RecyclerView.Adapter<WatchlistAdapter.StockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.symbol
        holder.binding.subtitleText.text = "${item.name} - ${item.price}"
    }

    override fun getItemCount(): Int = items.size

    class StockViewHolder(val binding: ItemStockBinding) : RecyclerView.ViewHolder(binding.root)
}

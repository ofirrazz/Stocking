package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemStockBinding
import com.stocksocial.model.Stock

class WatchlistAdapter : RecyclerView.Adapter<WatchlistAdapter.StockViewHolder>() {

    private val items = mutableListOf<Stock>()

    fun submitList(list: List<Stock>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val item = items[position]
        holder.binding.symbolText.text = item.symbol
        holder.binding.nameText.text = item.name
        holder.binding.priceText.text = "$${"%.2f".format(item.price)}"
        val change = "${if (item.dailyChangePercent >= 0) "+" else ""}${"%.2f".format(item.dailyChangePercent)}%"
        holder.binding.changeText.text = change
        holder.binding.changeText.setTextColor(
            holder.binding.root.context.getColor(
                if (item.dailyChangePercent >= 0) com.stocksocial.R.color.feed_accent_green
                else com.stocksocial.R.color.feed_accent_red
            )
        )
    }

    override fun getItemCount(): Int = items.size

    class StockViewHolder(val binding: ItemStockBinding) : RecyclerView.ViewHolder(binding.root)
}

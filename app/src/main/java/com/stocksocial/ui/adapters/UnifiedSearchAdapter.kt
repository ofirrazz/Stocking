package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemUnifiedSearchBinding
import com.stocksocial.model.SearchSuggestion
import com.stocksocial.model.SearchSuggestionType

class UnifiedSearchAdapter(
    private val onClick: (SearchSuggestion) -> Unit
) : RecyclerView.Adapter<UnifiedSearchAdapter.VH>() {

    private val items = mutableListOf<SearchSuggestion>()

    fun submitList(data: List<SearchSuggestion>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUnifiedSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.title
        holder.binding.subtitleText.text = item.subtitle
        holder.binding.typeChip.text = when (item.type) {
            SearchSuggestionType.USER -> "User"
            SearchSuggestionType.STOCK -> "Stock"
        }
        holder.binding.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemUnifiedSearchBinding) : RecyclerView.ViewHolder(binding.root)
}

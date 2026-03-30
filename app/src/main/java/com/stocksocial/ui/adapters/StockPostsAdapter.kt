package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemStockPostBinding
import com.stocksocial.model.Post
import com.stocksocial.ui.text.TickerSpannable
import java.util.Locale

class StockPostsAdapter(
    private val handleUsernameOverride: String? = null,
    private val onPostClick: (Post) -> Unit = {}
) : RecyclerView.Adapter<StockPostsAdapter.VH>() {
    private val items = mutableListOf<Post>()

    fun submitList(data: List<Post>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemStockPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx = holder.binding.root.context
        val rawName = item.author.displayName?.takeIf { it.isNotBlank() } ?: item.author.username
        holder.binding.authorText.text = rawName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val handle = handleUsernameOverride?.trim()?.removePrefix("@")?.ifBlank { null } ?: item.author.username
        holder.binding.handleText.text = "@$handle"
        holder.binding.timeText.text = item.createdAt
        holder.binding.contentText.text = TickerSpannable.format(ctx, item.content)
        holder.binding.root.setOnClickListener { onPostClick(item) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemStockPostBinding) : RecyclerView.ViewHolder(binding.root)
}

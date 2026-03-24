package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemPostBinding
import com.stocksocial.model.Post

class FeedAdapter(
    private val items: List<Post> = emptyList()
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.author.username
        holder.binding.subtitleText.text = item.content
    }

    override fun getItemCount(): Int = items.size

    class FeedViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)
}

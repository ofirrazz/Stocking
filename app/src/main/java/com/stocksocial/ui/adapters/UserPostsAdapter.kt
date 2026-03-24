package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemPostBinding
import com.stocksocial.model.Post

class UserPostsAdapter(
    private val items: List<Post> = emptyList()
) : RecyclerView.Adapter<UserPostsAdapter.UserPostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserPostViewHolder, position: Int) {
        val item = items[position]
        holder.binding.titleText.text = item.author.username
        holder.binding.subtitleText.text = item.content
    }

    override fun getItemCount(): Int = items.size

    class UserPostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)
}

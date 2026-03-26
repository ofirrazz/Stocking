package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stocksocial.databinding.ItemPostBinding
import com.stocksocial.model.Post

class FeedAdapter : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    private var posts: List<Post> = emptyList()

    fun setPosts(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val post = posts[position]
        
        holder.binding.titleText.text = post.authorName
        holder.binding.subtitleText.text = post.content
        
        // Show/Hide image based on URL availability
        if (!post.imageUrl.isNullOrEmpty()) {
            holder.binding.postImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .into(holder.binding.postImage)
        } else {
            holder.binding.postImage.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = posts.size

    class FeedViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)
}

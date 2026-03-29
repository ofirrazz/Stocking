package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stocksocial.databinding.ItemFeedPostBinding
import com.stocksocial.model.Post
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class FeedAdapter : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    private val items = mutableListOf<Post>()

    fun submitList(posts: List<Post>) {
        items.clear()
        items.addAll(posts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemFeedPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            usernameText.text = item.author.username
            timeText.text = item.createdAt
            postContentText.text = item.content
            likesCountText.text = item.likesCount.toString()
            commentsCountText.text = item.commentsCount.toString()

            val local = item.localImagePath
            val remote = item.imageUrl
            when {
                !local.isNullOrBlank() -> {
                    postImage.visibility = View.VISIBLE
                    Glide.with(postImage.context).load(File(local)).centerCrop().into(postImage)
                }
                !remote.isNullOrBlank() -> {
                    postImage.visibility = View.VISIBLE
                    Glide.with(postImage.context).load(remote).centerCrop().into(postImage)
                }
                else -> {
                    postImage.visibility = View.GONE
                    postImage.setImageDrawable(null)
                }
            }

            if (item.stockSymbol != null && item.stockPrice != null) {
                stockInfoContainer.visibility = View.VISIBLE
                stockSymbolText.text = item.stockSymbol
                stockPriceText.text = NumberFormat.getCurrencyInstance(Locale.US).format(item.stockPrice)
            } else {
                stockInfoContainer.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class FeedViewHolder(val binding: ItemFeedPostBinding) : RecyclerView.ViewHolder(binding.root)
}

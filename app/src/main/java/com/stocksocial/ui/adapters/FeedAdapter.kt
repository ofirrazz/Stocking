package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.ItemFeedPostBinding
import com.stocksocial.model.Post
import com.stocksocial.ui.text.TickerSpannable
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class FeedAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onStockClick: (String) -> Unit
) : ListAdapter<Post, FeedAdapter.FeedViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val binding = ItemFeedPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FeedViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            val name = item.author.username
            displayNameText.text = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            handleText.text = "@$name"
            timeText.text = item.createdAt
            postContentText.text = TickerSpannable.format(root.context, item.content)
            likesCountText.text = item.likesCount.toString()
            commentsCountText.text = item.commentsCount.toString()

            val avatar = item.author.avatarUrl
            if (!avatar.isNullOrBlank()) {
                Glide.with(profileImage).load(avatar).circleCrop().into(profileImage)
            } else {
                profileImage.setImageResource(android.R.drawable.ic_menu_myplaces)
            }

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
                stockInfoContainer.setOnClickListener { onStockClick(item.stockSymbol) }
            } else {
                stockInfoContainer.visibility = View.GONE
                stockInfoContainer.setOnClickListener(null)
            }

            root.setOnClickListener { onPostClick(item) }
            shareAction.setOnClickListener { onShareClick(item) }
        }
    }

    class FeedViewHolder(val binding: ItemFeedPostBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
        }
    }
}

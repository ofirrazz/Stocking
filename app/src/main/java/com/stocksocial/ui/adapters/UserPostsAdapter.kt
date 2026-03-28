package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemProfilePostBinding
import com.stocksocial.model.Post
import java.text.NumberFormat
import java.util.Locale

class UserPostsAdapter(
    private val items: List<Post> = emptyList()
) : RecyclerView.Adapter<UserPostsAdapter.UserPostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostViewHolder {
        val binding = ItemProfilePostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserPostViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            titleText.text = item.author.username
            timeText.text = item.createdAt
            subtitleText.text = item.content
            likesCountText.text = "${item.likesCount} likes"
            commentsCountText.text = "${item.commentsCount} comments"

            if (item.stockSymbol != null && item.stockPrice != null) {
                stockInfoContainer.visibility = View.VISIBLE
                stockSymbolText.text = item.stockSymbol
                stockPriceText.text = NumberFormat.getCurrencyInstance(Locale.US).format(item.stockPrice)
            } else {
                stockInfoContainer.visibility = View.GONE
            }

            imagePreviewContainer.visibility = if (item.imageUrl != null) View.VISIBLE else View.GONE
            videoPreviewContainer.visibility = if (item.videoUrl != null) View.VISIBLE else View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    class UserPostViewHolder(val binding: ItemProfilePostBinding) : RecyclerView.ViewHolder(binding.root)
}

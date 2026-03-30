package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stocksocial.databinding.ItemProfilePostBinding
import com.stocksocial.model.Post
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class UserPostsAdapter(
    private val onEditClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit
) : ListAdapter<Post, UserPostsAdapter.UserPostViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostViewHolder {
        val binding = ItemProfilePostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserPostViewHolder, position: Int) {
        val item = getItem(position)
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

            val hasImage = !item.imageUrl.isNullOrBlank() || !item.localImagePath.isNullOrBlank()
            imagePreviewContainer.visibility = if (hasImage) View.VISIBLE else View.GONE
            when {
                !item.localImagePath.isNullOrBlank() -> {
                    Glide.with(imagePreview.context)
                        .load(File(item.localImagePath))
                        .centerCrop()
                        .into(imagePreview)
                }
                !item.imageUrl.isNullOrBlank() -> {
                    Glide.with(imagePreview.context)
                        .load(item.imageUrl)
                        .centerCrop()
                        .into(imagePreview)
                }
                else -> imagePreview.setImageDrawable(null)
            }
            videoPreviewContainer.visibility = if (item.videoUrl != null) View.VISIBLE else View.GONE
            root.setOnClickListener { onEditClick(item) }
            commentAction.setOnClickListener { onEditClick(item) }
            likeAction.setOnClickListener { onLikeClick(item) }
            shareAction.setOnClickListener { onShareClick(item) }
        }
    }

    class UserPostViewHolder(val binding: ItemProfilePostBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
        }
    }
}

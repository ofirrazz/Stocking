package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stocksocial.databinding.ItemProfilePostBinding
import com.stocksocial.model.Post
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class UserPostsAdapter(
    private val onEdit: (Post) -> Unit,
    private val onDelete: (Post) -> Unit
) : RecyclerView.Adapter<UserPostsAdapter.UserPostViewHolder>() {

    private val items = mutableListOf<Post>()

    fun submitList(posts: List<Post>) {
        items.clear()
        items.addAll(posts)
        notifyDataSetChanged()
    }

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

            val hasImage = !item.imageUrl.isNullOrBlank() || !item.localImagePath.isNullOrBlank()
            imagePreviewContainer.visibility = if (hasImage) View.VISIBLE else View.GONE
            videoPreviewContainer.visibility = if (item.videoUrl != null) View.VISIBLE else View.GONE

            if (hasImage) {
                when {
                    !item.localImagePath.isNullOrBlank() ->
                        Glide.with(imagePreview).load(File(item.localImagePath!!)).centerCrop()
                            .into(imagePreview)
                    !item.imageUrl.isNullOrBlank() ->
                        Glide.with(imagePreview).load(item.imageUrl).centerCrop().into(imagePreview)
                }
            }

            ownerPostActions.visibility = View.VISIBLE
            editPostButton.setOnClickListener { onEdit(item) }
            deletePostButton.setOnClickListener { onDelete(item) }
        }
    }

    override fun getItemCount(): Int = items.size

    class UserPostViewHolder(val binding: ItemProfilePostBinding) : RecyclerView.ViewHolder(binding.root)
}

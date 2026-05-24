package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemFeedPostBinding
import com.stocksocial.model.Post
import com.stocksocial.ui.post.PostRowBinder

class UserPostsAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onEditClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onStockClick: (String) -> Unit
) : ListAdapter<Post, UserPostsAdapter.UserPostViewHolder>(DiffCallback) {

    var currentUserId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostViewHolder {
        val binding = ItemFeedPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserPostViewHolder, position: Int) {
        val item = getItem(position)
        PostRowBinder.bind(
            binding = holder.binding,
            item = item,
            currentUserId = currentUserId,
            onPostBodyClick = { onPostClick(item) },
            onLikeClick = { onLikeClick(item) },
            onCommentClick = { onCommentClick(item) },
            onEditClick = { onEditClick(item) },
            onShareClick = { onShareClick(item) },
            onStockClick = onStockClick
        )
    }

    class UserPostViewHolder(val binding: ItemFeedPostBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
        }
    }
}

package com.stocksocial.ui.post

import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.ItemFeedPostBinding
import com.stocksocial.model.Post
import com.stocksocial.ui.text.TickerSpannable
import java.io.File
import java.text.NumberFormat
import java.util.Locale

object PostRowBinder {

    fun bind(
        binding: ItemFeedPostBinding,
        item: Post,
        currentUserId: String?,
        onPostBodyClick: () -> Unit,
        onLikeClick: () -> Unit,
        onCommentClick: () -> Unit,
        onEditClick: () -> Unit,
        onShareClick: () -> Unit,
        onStockClick: (String) -> Unit
    ) {
        val ctx = binding.root.context
        val name = item.author.username
        binding.displayNameText.text =
            name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        binding.handleText.text = "@$name"
        binding.timeText.text = item.createdAt
        binding.postContentText.text = TickerSpannable.format(ctx, item.content)
        binding.likesCountText.text = item.likesCount.toString()
        binding.commentsCountText.text = item.commentsCount.toString()

        val gold = ContextCompat.getColor(ctx, R.color.primary_gold)
        val muted = ContextCompat.getColor(ctx, R.color.text_muted)
        if (item.likedByCurrentUser) {
            binding.likeStarIcon.setImageResource(android.R.drawable.btn_star_big_on)
            binding.likeStarIcon.imageTintList = ColorStateList.valueOf(gold)
        } else {
            binding.likeStarIcon.setImageResource(android.R.drawable.btn_star_big_off)
            binding.likeStarIcon.imageTintList = ColorStateList.valueOf(muted)
        }

        val isMine = currentUserId != null && item.author.id == currentUserId
        binding.editPostButton.visibility = if (isMine) View.VISIBLE else View.GONE

        val avatar = item.author.avatarUrl
        if (!avatar.isNullOrBlank()) {
            Glide.with(binding.profileImage).load(avatar).circleCrop().into(binding.profileImage)
        } else {
            binding.profileImage.setImageResource(android.R.drawable.ic_menu_myplaces)
        }

        val hasVideo = !item.videoUrl.isNullOrBlank()
        val local = item.localImagePath
        val remote = item.imageUrl
        when {
            hasVideo -> {
                binding.postImage.visibility = View.GONE
                binding.postImage.setImageDrawable(null)
                binding.videoPreviewContainer.visibility = View.VISIBLE
            }
            !local.isNullOrBlank() -> {
                binding.videoPreviewContainer.visibility = View.GONE
                binding.postImage.visibility = View.VISIBLE
                Glide.with(binding.postImage.context).load(File(local)).centerCrop().into(binding.postImage)
            }
            !remote.isNullOrBlank() -> {
                binding.videoPreviewContainer.visibility = View.GONE
                binding.postImage.visibility = View.VISIBLE
                Glide.with(binding.postImage.context).load(remote).centerCrop().into(binding.postImage)
            }
            else -> {
                binding.videoPreviewContainer.visibility = View.GONE
                binding.postImage.visibility = View.GONE
                binding.postImage.setImageDrawable(null)
            }
        }

        if (item.stockSymbol != null && item.stockPrice != null) {
            binding.stockInfoContainer.visibility = View.VISIBLE
            binding.stockSymbolText.text = item.stockSymbol
            binding.stockPriceText.text =
                NumberFormat.getCurrencyInstance(Locale.US).format(item.stockPrice)
            binding.stockInfoContainer.setOnClickListener { onStockClick(item.stockSymbol) }
        } else {
            binding.stockInfoContainer.visibility = View.GONE
            binding.stockInfoContainer.setOnClickListener(null)
        }

        binding.root.setOnClickListener { onPostBodyClick() }
        binding.likeAction.setOnClickListener { onLikeClick() }
        binding.commentAction.setOnClickListener { onCommentClick() }
        binding.editPostButton.setOnClickListener { onEditClick() }
        binding.shareAction.setOnClickListener { onShareClick() }
    }
}

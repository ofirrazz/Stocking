package com.stocksocial.ui.notifications

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stocksocial.R
import com.stocksocial.databinding.ItemNotificationBinding

enum class NotificationUiType {
    LIKE,
    COMMENT,
    FOLLOW,
    STOCK_ALERT,
    PRICE_ALERT
}

data class NotificationUi(
    val id: String,
    val type: NotificationUiType,
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean
)

class NotificationsAdapter : ListAdapter<NotificationUi, NotificationsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationUi) {
            val ctx = binding.root.context
            val color = ContextCompat.getColor(ctx, typeColorRes(item.type))

            val bg = GradientDrawable()
            bg.shape = GradientDrawable.OVAL
            bg.setColor(ColorUtils.setAlphaComponent(color, 40))
            binding.iconBackground.background = bg

            binding.typeIcon.setImageResource(typeIcon(item.type))
            binding.typeIcon.setColorFilter(color)

            binding.titleText.text = item.title
            binding.messageText.text = item.message
            binding.timeText.text = item.time

            val card = binding.root as MaterialCardView
            card.alpha = if (item.isRead) 0.72f else 1f
            card.strokeWidth = ctx.resources.getDimensionPixelSize(R.dimen.hairline)
            card.strokeColor = ContextCompat.getColor(ctx, R.color.border)

            binding.unreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE
        }

        private fun typeColorRes(type: NotificationUiType): Int = when (type) {
            NotificationUiType.LIKE -> R.color.primary_gold
            NotificationUiType.COMMENT -> R.color.silver
            NotificationUiType.FOLLOW -> R.color.accent_gold
            NotificationUiType.STOCK_ALERT -> R.color.success
            NotificationUiType.PRICE_ALERT -> R.color.destructive
        }

        private fun typeIcon(type: NotificationUiType): Int = when (type) {
            NotificationUiType.LIKE -> android.R.drawable.btn_star_big_on
            NotificationUiType.COMMENT -> android.R.drawable.ic_menu_edit
            NotificationUiType.FOLLOW -> android.R.drawable.ic_menu_myplaces
            NotificationUiType.STOCK_ALERT -> R.drawable.ic_trending_up_24
            NotificationUiType.PRICE_ALERT -> R.drawable.ic_notifications_24
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NotificationUi>() {
            override fun areItemsTheSame(a: NotificationUi, b: NotificationUi) = a.id == b.id
            override fun areContentsTheSame(a: NotificationUi, b: NotificationUi) = a == b
        }
    }
}

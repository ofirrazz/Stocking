package com.stocksocial.ui.adapters

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.ItemNotificationBinding
import com.stocksocial.model.InAppNotificationUi
import java.util.regex.Pattern

class NotificationsAdapter(
    private val onMarkRead: (String) -> Unit,
    private val onViewStock: (String) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.VH>() {

    private val items = mutableListOf<InAppNotificationUi>()
    private val tickerPattern = Pattern.compile("\\$[A-Za-z]{1,6}")

    fun submitList(data: List<InAppNotificationUi>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun updateItem(item: InAppNotificationUi) {
        val i = items.indexOfFirst { it.id == item.id }
        if (i >= 0) {
            items[i] = item
            notifyItemChanged(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val n = items[position]
        val ctx = holder.binding.root.context
        val pad = (4 * ctx.resources.displayMetrics.density).toInt()
        holder.binding.root.setBackgroundColor(
            ContextCompat.getColor(
                ctx,
                if (n.unread) R.color.notification_unread_bg else R.color.background
            )
        )
        holder.binding.headlineText.text = n.headline
        holder.binding.headlineText.setTypeface(null, Typeface.BOLD)
        holder.binding.bodyText.text = spanWithGoldTickers(n.body, ctx)
        holder.binding.timeText.text = n.timeLabel
        holder.binding.unreadDot.visibility = if (n.unread) View.VISIBLE else View.GONE

        if (n.isSocialStyle) {
            holder.binding.avatarImage.background = null
            holder.binding.avatarImage.setPadding(pad, pad, pad, pad)
            holder.binding.avatarImage.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(holder.binding.avatarImage)
                .load(R.drawable.ic_splash_mark)
                .circleCrop()
                .into(holder.binding.avatarImage)
        } else {
            Glide.with(holder.binding.avatarImage).clear(holder.binding.avatarImage)
            holder.binding.avatarImage.scaleType = ImageView.ScaleType.CENTER
            val inset = (10 * ctx.resources.displayMetrics.density).toInt()
            holder.binding.avatarImage.setPadding(inset, inset, inset, inset)
            holder.binding.avatarImage.background = ContextCompat.getDrawable(ctx, R.drawable.bg_notification_stock_avatar)
            holder.binding.avatarImage.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_notification_dollar))
        }

        val sym = n.stockSymbolForView
        when {
            sym != null && n.viewPositive != null -> {
                holder.binding.viewChip.visibility = View.VISIBLE
                val green = ContextCompat.getColor(ctx, R.color.success_green)
                val red = ContextCompat.getColor(ctx, R.color.destructive_red)
                if (n.viewPositive) {
                    holder.binding.viewChip.setTextColor(green)
                    holder.binding.viewChip.strokeColor = ColorStateList.valueOf(green)
                    holder.binding.viewChip.iconTint = ColorStateList.valueOf(green)
                    holder.binding.viewChip.setIconResource(android.R.drawable.arrow_up_float)
                } else {
                    holder.binding.viewChip.setTextColor(red)
                    holder.binding.viewChip.strokeColor = ColorStateList.valueOf(red)
                    holder.binding.viewChip.iconTint = ColorStateList.valueOf(red)
                    holder.binding.viewChip.setIconResource(android.R.drawable.arrow_down_float)
                }
                holder.binding.viewChip.setOnClickListener {
                    onViewStock(sym)
                }
            }
            else -> {
                holder.binding.viewChip.visibility = View.GONE
                holder.binding.viewChip.setOnClickListener(null)
            }
        }

        holder.binding.root.setOnClickListener {
            if (n.unread) onMarkRead(n.id)
        }
    }

    private fun spanWithGoldTickers(text: String, ctx: android.content.Context): SpannableString {
        val ss = SpannableString(text)
        val gold = ContextCompat.getColor(ctx, R.color.primary_gold)
        val m = tickerPattern.matcher(text)
        while (m.find()) {
            ss.setSpan(ForegroundColorSpan(gold), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            ss.setSpan(StyleSpan(Typeface.BOLD), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return ss
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)
}

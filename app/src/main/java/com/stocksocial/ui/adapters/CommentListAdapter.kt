package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemCommentRowBinding
import com.stocksocial.model.PostComment

class CommentListAdapter : ListAdapter<PostComment, CommentListAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCommentRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.commentAuthorText.text = "@${item.authorUsername}"
        holder.binding.commentTimeText.text = item.createdAt
        holder.binding.commentBodyText.text = item.content
    }

    class VH(val binding: ItemCommentRowBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val Diff = object : DiffUtil.ItemCallback<PostComment>() {
            override fun areItemsTheSame(a: PostComment, b: PostComment) = a.id == b.id
            override fun areContentsTheSame(a: PostComment, b: PostComment) = a == b
        }
    }
}

package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemUserSearchBinding
import com.stocksocial.model.UserSuggestion

class UserSearchAdapter(
    private val onUserSelected: (UserSuggestion) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.VH>() {

    private val items = mutableListOf<UserSuggestion>()

    fun submitList(data: List<UserSuggestion>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.usernameText.text = "@${item.username}"
        holder.binding.root.setOnClickListener { onUserSelected(item) }
    }

    override fun getItemCount(): Int = items.size

    class VH(val binding: ItemUserSearchBinding) : RecyclerView.ViewHolder(binding.root)
}

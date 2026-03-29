package com.stocksocial.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.stocksocial.databinding.ItemArticleCardBinding
import com.stocksocial.model.Article

class ArticlesAdapter(
    private val onArticleClick: (Article) -> Unit
) : RecyclerView.Adapter<ArticlesAdapter.ArticleViewHolder>() {

    private val items = mutableListOf<Article>()

    fun submitList(articles: List<Article>) {
        items.clear()
        items.addAll(articles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemArticleCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            categoryChip.text = item.category
            titleText.text = item.title
            previewText.text = item.summary
            sourceText.text = item.source
            timeText.text = item.publishedAt ?: ""
            root.setOnClickListener { onArticleClick(item) }
        }
    }

    override fun getItemCount(): Int = items.size

    class ArticleViewHolder(val binding: ItemArticleCardBinding) : RecyclerView.ViewHolder(binding.root)
}

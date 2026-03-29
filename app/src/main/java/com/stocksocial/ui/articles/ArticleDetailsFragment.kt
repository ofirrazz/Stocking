package com.stocksocial.ui.articles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.stocksocial.R
import com.stocksocial.databinding.FragmentArticleDetailsBinding
import com.stocksocial.model.Article
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.ArticlesViewModel
import kotlinx.coroutines.launch

class ArticleDetailsFragment : Fragment() {

    private val viewModel: ArticlesViewModel by viewModels { appViewModelFactory }
    private var _binding: FragmentArticleDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticleDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val articleId = requireArguments().getString("articleId").orEmpty()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.articleDetailsState.collect { state ->
                    state.errorMessage?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                    state.data?.let { article -> bindArticle(article) }
                }
            }
        }

        if (articleId.isNotEmpty()) {
            viewModel.loadArticleDetails(articleId)
        }
    }

    private fun bindArticle(article: Article) {
        binding.categoryText.text = article.category
        binding.articleTitleText.text = article.title
        binding.authorText.text = getString(R.string.article_author_format, article.author)
        binding.sourceText.text = article.source
        binding.publishedTimeText.text = article.publishedAt ?: ""
        binding.articleContentText.text = article.content
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

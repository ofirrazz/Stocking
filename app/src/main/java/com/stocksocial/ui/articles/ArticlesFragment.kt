package com.stocksocial.ui.articles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.stocksocial.R
import com.stocksocial.databinding.FragmentArticlesBinding
import com.stocksocial.model.Article
import com.stocksocial.ui.adapters.ArticlesAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.ArticlesViewModel
import kotlinx.coroutines.launch

class ArticlesFragment : Fragment() {

    private val viewModel: ArticlesViewModel by viewModels { appViewModelFactory }
    private val articlesAdapter = ArticlesAdapter { article ->
        findNavController().navigate(
            R.id.action_articlesFragment_to_articleDetailsFragment,
            bundleOf("articleId" to article.id)
        )
    }
    private var allArticles = emptyList<Article>()
    private var _binding: FragmentArticlesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArticlesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.articlesRecyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.articlesRecyclerView.adapter = articlesAdapter

        setupCategoryFiltering()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.articlesState.collect { state ->
                    allArticles = state.data.orEmpty()
                    applyFilter(getSelectedCategory())
                }
            }
        }

        viewModel.loadArticles()
    }

    private fun setupCategoryFiltering() {
        binding.categoryChipGroup.setOnCheckedStateChangeListener { _, _ ->
            applyFilter(getSelectedCategory())
        }
    }

    private fun getSelectedCategory(): String {
        return when (binding.categoryChipGroup.checkedChipId) {
            R.id.chipEarnings -> getString(R.string.category_earnings)
            R.id.chipEconomics -> getString(R.string.category_economics)
            R.id.chipTechnology -> getString(R.string.category_technology)
            R.id.chipAutomotive -> getString(R.string.category_automotive)
            else -> getString(R.string.category_all)
        }
    }

    private fun applyFilter(category: String) {
        val filtered = if (category == getString(R.string.category_all)) {
            allArticles
        } else {
            allArticles.filter { it.category.equals(category, ignoreCase = true) }
        }
        articlesAdapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

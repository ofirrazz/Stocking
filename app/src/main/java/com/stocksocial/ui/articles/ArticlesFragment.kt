package com.stocksocial.ui.articles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stocksocial.R
import com.stocksocial.databinding.FragmentArticlesBinding
import com.stocksocial.ui.adapters.ArticlesAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.ArticlesViewModel

class ArticlesFragment : Fragment() {

    private val viewModel: ArticlesViewModel by viewModels { appViewModelFactory }
    private val articlesAdapter = ArticlesAdapter { article ->
        val direction = ArticlesFragmentDirections
            .actionArticlesFragmentToArticleDetailsFragment(article.id)
        findNavController().navigate(direction)
    }
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

        viewModel.filteredArticlesState.observe(viewLifecycleOwner) { state ->
            articlesAdapter.submitList(state.data.orEmpty())
        }

        viewModel.loadArticles()
    }

    private fun setupCategoryFiltering() {
        binding.categoryChipGroup.setOnCheckedStateChangeListener { _, _ ->
            viewModel.setCategoryFilter(getSelectedCategory())
        }
    }

    private fun getSelectedCategory(): String? {
        return when (binding.categoryChipGroup.checkedChipId) {
            R.id.chipEarnings -> getString(R.string.category_earnings)
            R.id.chipEconomics -> getString(R.string.category_economics)
            R.id.chipTechnology -> getString(R.string.category_technology)
            R.id.chipAutomotive -> getString(R.string.category_automotive)
            else -> null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

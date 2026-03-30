package com.stocksocial.ui.articles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
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
    private var lastShownError: String? = null
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

        viewModel.articlesStateLive.observe(viewLifecycleOwner) { state ->
            binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            articlesAdapter.submitList(state.data.orEmpty())
            val error = state.errorMessage
            if (!error.isNullOrBlank() && error != lastShownError) {
                lastShownError = error
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadArticles()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

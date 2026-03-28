package com.stocksocial.ui.articles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.stocksocial.databinding.FragmentArticlesBinding
import com.stocksocial.utils.appContainer
import com.stocksocial.viewmodel.AppViewModelFactory
import com.stocksocial.viewmodel.ArticlesViewModel

class ArticlesFragment : Fragment() {

    private val viewModel: ArticlesViewModel by viewModels {
        AppViewModelFactory(articlesRepository = appContainer.articlesRepository)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

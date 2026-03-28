package com.stocksocial.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.databinding.FragmentFeedBinding
import com.stocksocial.ui.adapters.FeedAdapter
import com.stocksocial.utils.appContainer
import com.stocksocial.viewmodel.AppViewModelFactory
import com.stocksocial.viewmodel.FeedViewModel
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels {
        AppViewModelFactory(feedRepository = appContainer.feedRepository)
    }
    private val feedAdapter = FeedAdapter()
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.feedRecyclerView.adapter = feedAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.feedState.collect { state ->
                    binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    state.data?.let { posts -> feedAdapter.submitList(posts) }
                }
            }
        }

        viewModel.refreshMockFeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

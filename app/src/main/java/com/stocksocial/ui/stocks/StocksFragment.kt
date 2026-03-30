package com.stocksocial.ui.stocks

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.R
import com.stocksocial.databinding.FragmentStocksBinding
import com.stocksocial.ui.adapters.MarketIndexAdapter
import com.stocksocial.ui.adapters.TopSignalsAdapter
import com.stocksocial.ui.adapters.TrendingStocksAdapter
import com.stocksocial.ui.adapters.WatchlistAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.StocksViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StocksFragment : Fragment() {

    private val viewModel: StocksViewModel by viewModels { appViewModelFactory }
    private val marketIndexAdapter = MarketIndexAdapter { openStockDetails(it.symbol) }
    private val trendingStocksAdapter = TrendingStocksAdapter { openStockDetails(it.symbol) }
    private val watchlistAdapter = WatchlistAdapter { openStockDetails(it.symbol) }
    private val recentlyAdapter = WatchlistAdapter { openStockDetails(it.symbol) }
    private val topSignalsAdapter = TopSignalsAdapter { openStockDetails(it) }
    private var lastShownError: String? = null
    private var marketRefreshJob: Job? = null
    private var _binding: FragmentStocksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStocksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.marketIndicesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.marketIndicesRecyclerView.adapter = marketIndexAdapter

        binding.trendingRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.trendingRecyclerView.adapter = trendingStocksAdapter

        binding.watchlistRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.watchlistRecyclerView.adapter = watchlistAdapter

        binding.recentlyRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recentlyRecyclerView.adapter = recentlyAdapter

        binding.topSignalsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.topSignalsRecyclerView.adapter = topSignalsAdapter

        binding.searchInputLayout.setEndIconOnClickListener {
            submitSearch()
        }
        binding.searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                submitSearch()
                true
            } else {
                false
            }
        }

        viewModel.stocksStateLive.observe(viewLifecycleOwner) { state ->
            val data = state.data ?: return@observe
            marketIndexAdapter.submitList(data.marketIndices)
            trendingStocksAdapter.submitList(data.trendingStocks)
            watchlistAdapter.submitList(data.watchlist)
            recentlyAdapter.submitList(data.recentSearches)
            topSignalsAdapter.submitList(data.topSignals)
            binding.recentlySectionTitle.visibility =
                if (data.recentSearches.isEmpty()) View.GONE else View.VISIBLE
            binding.recentlyRecyclerView.visibility =
                if (data.recentSearches.isEmpty()) View.GONE else View.VISIBLE
            val error = state.errorMessage
            if (!error.isNullOrBlank() && error != lastShownError) {
                lastShownError = error
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.loadStocks()
    }

    override fun onStart() {
        super.onStart()
        marketRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(45_000)
                viewModel.refreshMarketSnapshot()
            }
        }
    }

    override fun onStop() {
        marketRefreshJob?.cancel()
        marketRefreshJob = null
        super.onStop()
    }

    private fun submitSearch() {
        val query = binding.searchInput.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            Toast.makeText(requireContext(), R.string.enter_stock_symbol, Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.searchStock(query)
        binding.searchInput.text?.clear()
        binding.searchInput.clearFocus()
    }

    private fun openStockDetails(symbol: String) {
        val action = StocksFragmentDirections.actionStocksFragmentToStockDetailsFragment(symbol.uppercase())
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

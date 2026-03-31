package com.stocksocial.ui.hotstocks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.stocksocial.databinding.FragmentHotStocksBinding
import com.stocksocial.ui.adapters.GroupedHotStocksAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.StocksViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HotStocksFragment : Fragment() {

    private val stocksViewModel: StocksViewModel by viewModels { appViewModelFactory }
    private val hotStocksAdapter = GroupedHotStocksAdapter(
        onStockClick = { symbol ->
            val dir = HotStocksFragmentDirections.actionHotStocksFragmentToStockDetailsFragment(symbol)
            findNavController().navigate(dir)
        },
        onFavoriteClick = { stock -> stocksViewModel.toggleHotStockFavorite(stock) }
    )
    private var marketRefreshJob: Job? = null
    private var _binding: FragmentHotStocksBinding? = null
    private val binding get() = _binding!!
    private var searchFilter: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHotStocksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.hotStocksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.hotStocksRecyclerView.adapter = hotStocksAdapter

        binding.hotSearchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitSearch()
                true
            } else {
                false
            }
        }

        binding.hotSearchLayout.setEndIconOnClickListener { submitSearch() }

        binding.hotSearchEdit.doAfterTextChanged { text ->
            searchFilter = text?.toString().orEmpty()
            stocksViewModel.onTradingSearchQueryChanged(searchFilter)
            refreshGroupedList()
        }

        stocksViewModel.stocksStateLive.observe(viewLifecycleOwner) { state ->
            binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.errorMessage?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                stocksViewModel.consumeError()
            }
            state.data?.let { data ->
                hotStocksAdapter.submit(HotStocksUiBuilder.build(data, searchFilter))
            }
        }

        stocksViewModel.favoriteToggleErrorLive.observe(viewLifecycleOwner) { msg ->
            if (msg.isNullOrEmpty()) return@observe
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            stocksViewModel.consumeFavoriteToggleError()
        }

        stocksViewModel.loadStocks()
    }

    private fun submitSearch() {
        val q = binding.hotSearchEdit.text?.toString().orEmpty().trim()
        if (q.isEmpty()) return
        stocksViewModel.searchStock(q)
    }

    private fun refreshGroupedList() {
        val data = stocksViewModel.stocksStateLive.value?.data ?: return
        hotStocksAdapter.submit(HotStocksUiBuilder.build(data, searchFilter))
    }

    override fun onStart() {
        super.onStart()
        marketRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(45_000)
                stocksViewModel.refreshMarketSnapshot()
            }
        }
        // Avoid duplicate Finnhub burst: loadStocks() already filled quotes in onViewCreated.
    }

    override fun onStop() {
        marketRefreshJob?.cancel()
        marketRefreshJob = null
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

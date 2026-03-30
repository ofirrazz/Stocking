package com.stocksocial.ui.stocks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.databinding.FragmentStocksBinding
import com.stocksocial.ui.adapters.MarketIndexAdapter
import com.stocksocial.ui.adapters.TopSignalsAdapter
import com.stocksocial.ui.adapters.TrendingStocksAdapter
import com.stocksocial.ui.adapters.WatchlistAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.StocksViewModel

class StocksFragment : Fragment() {

    private val viewModel: StocksViewModel by viewModels { appViewModelFactory }
    private val marketIndexAdapter = MarketIndexAdapter()
    private val trendingStocksAdapter = TrendingStocksAdapter()
    private val watchlistAdapter = WatchlistAdapter()
    private val topSignalsAdapter = TopSignalsAdapter()
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

        binding.topSignalsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.topSignalsRecyclerView.adapter = topSignalsAdapter

        viewModel.stocksStateLive.observe(viewLifecycleOwner) { state ->
            val data = state.data ?: return@observe
            marketIndexAdapter.submitList(data.marketIndices)
            trendingStocksAdapter.submitList(data.trendingStocks)
            watchlistAdapter.submitList(data.watchlist)
            topSignalsAdapter.submitList(data.topSignals)
        }

        viewModel.loadStocks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

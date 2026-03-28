package com.stocksocial.ui.stocks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.databinding.FragmentStocksBinding
import com.stocksocial.ui.adapters.WatchlistAdapter
import com.stocksocial.utils.appContainer
import com.stocksocial.utils.DummyData
import com.stocksocial.viewmodel.AppViewModelFactory
import com.stocksocial.viewmodel.StocksViewModel

class StocksFragment : Fragment() {

    private val viewModel: StocksViewModel by viewModels {
        AppViewModelFactory(watchlistRepository = appContainer.watchlistRepository)
    }
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

        binding.watchlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.watchlistRecyclerView.adapter = WatchlistAdapter(DummyData.watchlistStocks())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

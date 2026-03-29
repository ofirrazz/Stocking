package com.stocksocial.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Stock
import com.stocksocial.model.StockSignal
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.repository.WatchlistRepository
import com.stocksocial.utils.DummyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StocksViewModel(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _stocksState = MutableStateFlow(UiState(data = StocksUiData()))
    val stocksState: StateFlow<UiState<StocksUiData>> = _stocksState.asStateFlow()

    fun loadStocks() {
        viewModelScope.launch {
            _stocksState.value = UiState(isLoading = true)
            when (val result = watchlistRepository.getWatchlist()) {
                is RepositoryResult.Success -> {
                    _stocksState.value = UiState(
                        data = buildMockData(watchlistOverride = result.data.map { it.stock })
                    )
                }
                is RepositoryResult.Error -> {
                    _stocksState.value = UiState(
                        data = buildMockData(),
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun loadMockStocks() {
        _stocksState.value = UiState(data = buildMockData())
    }

    private fun buildMockData(watchlistOverride: List<Stock>? = null): StocksUiData {
        return StocksUiData(
            marketIndices = DummyData.marketIndices(),
            trendingStocks = DummyData.trendingStocks(),
            watchlist = watchlistOverride ?: DummyData.watchlistStocks(),
            topSignals = DummyData.topSignals()
        )
    }
}

data class StocksUiData(
    val marketIndices: List<Stock> = emptyList(),
    val trendingStocks: List<Stock> = emptyList(),
    val watchlist: List<Stock> = emptyList(),
    val topSignals: List<StockSignal> = emptyList()
)

package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Stock
import com.stocksocial.model.StockSignal
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.repository.WatchlistRepository
import com.stocksocial.utils.DummyData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StocksViewModel(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _stocksState = MutableStateFlow(UiState(data = StocksUiData()))
    val stocksState: StateFlow<UiState<StocksUiData>> = _stocksState.asStateFlow()
    val stocksStateLive: LiveData<UiState<StocksUiData>> = _stocksState.asLiveData()
    private val recentSearches = mutableListOf<Stock>()

    fun loadStocks() {
        viewModelScope.launch {
            _stocksState.value = UiState(isLoading = true)

            val fallback = buildMockData()
            val (marketResult, trendingResult, watchlistResult) = coroutineScope {
                val marketDeferred = async { watchlistRepository.getMarketIndices() }
                val trendingDeferred = async { watchlistRepository.getTrendingStocks() }
                val watchlistDeferred = async { watchlistRepository.getWatchlist() }
                Triple(
                    marketDeferred.await(),
                    trendingDeferred.await(),
                    watchlistDeferred.await()
                )
            }

            val marketIndices = when (marketResult) {
                is RepositoryResult.Success ->
                    marketResult.data.takeIf { it.isNotEmpty() } ?: fallback.marketIndices
                is RepositoryResult.Error -> fallback.marketIndices
            }

            val trendingStocks = when (trendingResult) {
                is RepositoryResult.Success ->
                    trendingResult.data.takeIf { it.isNotEmpty() } ?: fallback.trendingStocks
                is RepositoryResult.Error -> fallback.trendingStocks
            }

            val watchlistStocks = when (watchlistResult) {
                is RepositoryResult.Success ->
                    watchlistResult.data.map { it.stock }.takeIf { it.isNotEmpty() } ?: fallback.watchlist
                is RepositoryResult.Error -> fallback.watchlist
            }

            val firstError = listOf(marketResult, trendingResult, watchlistResult)
                .filterIsInstance<RepositoryResult.Error>()
                .firstOrNull()
                ?.message

            _stocksState.value = UiState(
                data = StocksUiData(
                    marketIndices = marketIndices,
                    trendingStocks = trendingStocks,
                    watchlist = watchlistStocks,
                    topSignals = fallback.topSignals,
                    recentSearches = recentSearches.toList()
                ),
                errorMessage = firstError
            )
        }
    }

    fun refreshMarketSnapshot() {
        viewModelScope.launch {
            val prev = _stocksState.value.data ?: StocksUiData()
            val marketResult = watchlistRepository.getMarketIndices()
            val trendingResult = watchlistRepository.getTrendingStocks()
            val watchlistResult = watchlistRepository.getWatchlist()
            val fallback = buildMockData(prev.watchlist.takeIf { it.isNotEmpty() })
            val marketIndices = when (marketResult) {
                is RepositoryResult.Success ->
                    marketResult.data.takeIf { it.isNotEmpty() } ?: prev.marketIndices.ifEmpty { fallback.marketIndices }
                is RepositoryResult.Error -> prev.marketIndices.ifEmpty { fallback.marketIndices }
            }
            val trendingStocks = when (trendingResult) {
                is RepositoryResult.Success ->
                    trendingResult.data.takeIf { it.isNotEmpty() } ?: prev.trendingStocks.ifEmpty { fallback.trendingStocks }
                is RepositoryResult.Error -> prev.trendingStocks.ifEmpty { fallback.trendingStocks }
            }
            val watchlistStocks = when (watchlistResult) {
                is RepositoryResult.Success ->
                    watchlistResult.data.map { it.stock }.takeIf { it.isNotEmpty() }
                        ?: prev.watchlist.ifEmpty { fallback.watchlist }
                is RepositoryResult.Error -> prev.watchlist.ifEmpty { fallback.watchlist }
            }
            _stocksState.value = UiState(
                data = prev.copy(
                    marketIndices = marketIndices,
                    trendingStocks = trendingStocks,
                    watchlist = watchlistStocks
                )
            )
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
            topSignals = DummyData.topSignals(),
            recentSearches = recentSearches.toList()
        )
    }

    fun searchStock(symbolQuery: String) {
        viewModelScope.launch {
            val currentData = _stocksState.value.data ?: StocksUiData()
            when (val result = watchlistRepository.getStockBySymbol(symbolQuery)) {
                is RepositoryResult.Success -> {
                    val found = result.data
                    recentSearches.removeAll { it.symbol.equals(found.symbol, ignoreCase = true) }
                    recentSearches.add(0, found)
                    if (recentSearches.size > 8) {
                        recentSearches.removeLast()
                    }
                    _stocksState.value = UiState(
                        data = currentData.copy(recentSearches = recentSearches.toList())
                    )
                }
                is RepositoryResult.Error -> {
                    _stocksState.value = UiState(
                        data = currentData,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}

data class StocksUiData(
    val marketIndices: List<Stock> = emptyList(),
    val trendingStocks: List<Stock> = emptyList(),
    val watchlist: List<Stock> = emptyList(),
    val topSignals: List<StockSignal> = emptyList(),
    val recentSearches: List<Stock> = emptyList()
)

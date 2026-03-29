package com.stocksocial.viewmodel

import androidx.lifecycle.ViewModel
import com.stocksocial.model.Stock
import com.stocksocial.model.StockSignal
import com.stocksocial.utils.DummyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StocksViewModel : ViewModel() {

    private val _stocksState = MutableStateFlow(UiState(data = StocksUiData()))
    val stocksState: StateFlow<UiState<StocksUiData>> = _stocksState.asStateFlow()

    fun loadStocks() {
        _stocksState.value = UiState(
            data = StocksUiData(
                marketIndices = DummyData.marketIndices(),
                trendingStocks = DummyData.trendingStocks(),
                watchlist = DummyData.watchlistStocks(),
                topSignals = DummyData.topSignals()
            )
        )
    }
}

data class StocksUiData(
    val marketIndices: List<Stock> = emptyList(),
    val trendingStocks: List<Stock> = emptyList(),
    val watchlist: List<Stock> = emptyList(),
    val topSignals: List<StockSignal> = emptyList()
)

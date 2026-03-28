package com.stocksocial.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.WatchlistItem
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.repository.WatchlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StocksViewModel(
    private val watchlistRepository: WatchlistRepository? = null
) : ViewModel() {

    private val _stocksState = MutableStateFlow(UiState<List<WatchlistItem>>())
    val stocksState: StateFlow<UiState<List<WatchlistItem>>> = _stocksState.asStateFlow()

    fun loadStocks() {
        val repository = watchlistRepository ?: run {
            _stocksState.value = UiState(errorMessage = "WatchlistRepository is not attached")
            return
        }

        viewModelScope.launch {
            _stocksState.value = UiState(isLoading = true)
            when (val result = repository.getWatchlist()) {
                is RepositoryResult.Success -> _stocksState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _stocksState.value = UiState(errorMessage = result.message)
            }
        }
    }
}

package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.WatchlistItem
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.repository.WatchlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WatchlistViewModel(
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _watchlistState = MutableStateFlow(UiState<List<WatchlistItem>>())
    val watchlistState: StateFlow<UiState<List<WatchlistItem>>> = _watchlistState.asStateFlow()
    val watchlistStateLive: LiveData<UiState<List<WatchlistItem>>> = _watchlistState.asLiveData()

    fun loadWatchlist() {
        viewModelScope.launch {
            _watchlistState.value = UiState(isLoading = true)
            when (val result = watchlistRepository.getWatchlist()) {
                is RepositoryResult.Success -> {
                    _watchlistState.value = UiState(data = result.data)
                }
                is RepositoryResult.Error -> {
                    _watchlistState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }
}

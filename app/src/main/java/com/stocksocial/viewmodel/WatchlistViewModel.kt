package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.WatchlistItem
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.repository.WatchlistRepository
import kotlinx.coroutines.launch

class WatchlistViewModel(
    private val repository: WatchlistRepository = WatchlistRepository()
) : ViewModel() {

    private val _items = MutableLiveData<List<WatchlistItem>>(emptyList())
    val items: LiveData<List<WatchlistItem>> = _items

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun load(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getWatchlist(userId)) {
                is RepositoryResult.Success -> _items.value = result.data
                is RepositoryResult.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

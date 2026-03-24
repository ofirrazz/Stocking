package com.stocksocial.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Post
import com.stocksocial.repository.FeedRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedRepository: FeedRepository? = null
) : ViewModel() {

    private val _feedState = MutableStateFlow(UiState<List<Post>>())
    val feedState: StateFlow<UiState<List<Post>>> = _feedState.asStateFlow()

    fun loadFeed() {
        val repository = feedRepository ?: run {
            _feedState.value = UiState(errorMessage = "FeedRepository is not attached")
            return
        }

        viewModelScope.launch {
            _feedState.value = UiState(isLoading = true)
            when (val result = repository.getFeedPosts()) {
                is RepositoryResult.Success -> _feedState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _feedState.value = UiState(errorMessage = result.message)
            }
        }
    }
}

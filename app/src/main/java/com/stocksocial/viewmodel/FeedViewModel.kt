package com.stocksocial.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Post
import com.stocksocial.repository.FeedRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _feedState = MutableStateFlow(UiState<List<Post>>())
    val feedState: StateFlow<UiState<List<Post>>> = _feedState.asStateFlow()
    val feedStateLive: LiveData<UiState<List<Post>>> = _feedState.asLiveData()

    private val _postPublished = MutableStateFlow(false)
    val postPublished: StateFlow<Boolean> = _postPublished.asStateFlow()

    private val _publishError = MutableStateFlow<String?>(null)
    val publishError: StateFlow<String?> = _publishError.asStateFlow()

    fun loadFeed() {
        viewModelScope.launch {
            _feedState.value = UiState(isLoading = true)
            when (val result = feedRepository.getFeedPosts()) {
                is RepositoryResult.Success -> {
                    _feedState.value = UiState(data = result.data)
                }
                is RepositoryResult.Error -> {
                    _feedState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun publishPost(content: String, imageUri: Uri?) {
        viewModelScope.launch {
            _publishError.value = null
            when (val result = feedRepository.publishPost(content, imageUri)) {
                is RepositoryResult.Success -> {
                    _postPublished.value = true
                    loadFeed()
                }
                is RepositoryResult.Error -> {
                    _publishError.value = result.message
                }
            }
        }
    }

    fun consumePostPublished() {
        _postPublished.value = false
    }

    fun consumePublishError() {
        _publishError.value = null
    }
}

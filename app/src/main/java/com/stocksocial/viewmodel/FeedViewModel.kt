package com.stocksocial.viewmodel

import android.net.Uri
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
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _feedState = MutableStateFlow(UiState<List<Post>>())
    val feedState: StateFlow<UiState<List<Post>>> = _feedState.asStateFlow()

    private val _postPublished = MutableStateFlow(false)
    val postPublished: StateFlow<Boolean> = _postPublished.asStateFlow()

    private val _publishError = MutableStateFlow<String?>(null)
    val publishError: StateFlow<String?> = _publishError.asStateFlow()

    private val _postWriteBusy = MutableStateFlow(false)
    val postWriteBusy: StateFlow<Boolean> = _postWriteBusy.asStateFlow()

    private val _postForEdit = MutableStateFlow<Post?>(null)
    val postForEdit: StateFlow<Post?> = _postForEdit.asStateFlow()

    private val _editPostLoadError = MutableStateFlow<String?>(null)
    val editPostLoadError: StateFlow<String?> = _editPostLoadError.asStateFlow()

    private val _postDeletedId = MutableStateFlow<String?>(null)
    val postDeletedId: StateFlow<String?> = _postDeletedId.asStateFlow()

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

    fun prepareEditPost(postId: String?) {
        if (postId.isNullOrBlank()) {
            _postForEdit.value = null
            _editPostLoadError.value = null
            return
        }
        viewModelScope.launch {
            _editPostLoadError.value = null
            _postForEdit.value = null
            when (val result = feedRepository.getPostForEdit(postId)) {
                is RepositoryResult.Success -> {
                    _postForEdit.value = result.data
                }
                is RepositoryResult.Error -> {
                    _editPostLoadError.value = result.message
                }
            }
        }
    }

    fun consumePostForEdit() {
        _postForEdit.value = null
    }

    fun consumeEditPostLoadError() {
        _editPostLoadError.value = null
    }

    fun publishPost(content: String, imageUri: Uri?) {
        viewModelScope.launch {
            _publishError.value = null
            _postWriteBusy.value = true
            when (val result = feedRepository.publishPost(content, imageUri)) {
                is RepositoryResult.Success -> {
                    _postPublished.value = true
                    loadFeed()
                }
                is RepositoryResult.Error -> {
                    _publishError.value = result.message
                }
            }
            _postWriteBusy.value = false
        }
    }

    fun updatePost(postId: String, content: String, imageUri: Uri?, removeImage: Boolean) {
        viewModelScope.launch {
            _publishError.value = null
            _postWriteBusy.value = true
            when (val result = feedRepository.updatePost(postId, content, imageUri, removeImage)) {
                is RepositoryResult.Success -> {
                    _postPublished.value = true
                    loadFeed()
                }
                is RepositoryResult.Error -> {
                    _publishError.value = result.message
                }
            }
            _postWriteBusy.value = false
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _publishError.value = null
            _postWriteBusy.value = true
            when (val result = feedRepository.deletePost(postId)) {
                is RepositoryResult.Success -> {
                    _postDeletedId.value = postId
                    loadFeed()
                }
                is RepositoryResult.Error -> {
                    _publishError.value = result.message
                }
            }
            _postWriteBusy.value = false
        }
    }

    fun consumePostDeleted() {
        _postDeletedId.value = null
    }

    fun consumePostPublished() {
        _postPublished.value = false
    }

    fun consumePublishError() {
        _publishError.value = null
    }
}

package com.stocksocial.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Post
import com.stocksocial.model.PostComment
import com.stocksocial.repository.FeedRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FeedViewModel(
    private val feedRepository: FeedRepository
) : ViewModel() {

    private val _feedState = MutableStateFlow(UiState<List<Post>>())
    val feedState: StateFlow<UiState<List<Post>>> = _feedState.asStateFlow()
    val feedStateLive: LiveData<UiState<List<Post>>> = _feedState.asLiveData()

    private val _postPublished = MutableStateFlow(false)
    val postPublished: StateFlow<Boolean> = _postPublished.asStateFlow()
    val postPublishedLive: LiveData<Boolean> = _postPublished.asLiveData()

    private val _publishError = MutableStateFlow<String?>(null)
    val publishError: StateFlow<String?> = _publishError.asStateFlow()
    val publishErrorLive: LiveData<String?> = _publishError.asLiveData()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()
    val isPublishingLive: LiveData<Boolean> = _isPublishing.asLiveData()

    private val _postDetailsState = MutableStateFlow(UiState<Post>())
    val postDetailsState: StateFlow<UiState<Post>> = _postDetailsState.asStateFlow()
    val postDetailsStateLive: LiveData<UiState<Post>> = _postDetailsState.asLiveData()

    private val _postActionState = MutableStateFlow(UiState<Unit>())
    val postActionState: StateFlow<UiState<Unit>> = _postActionState.asStateFlow()
    val postActionStateLive: LiveData<UiState<Unit>> = _postActionState.asLiveData()

    private val _commentsState = MutableStateFlow(UiState<List<PostComment>>())
    val commentsState: StateFlow<UiState<List<PostComment>>> = _commentsState.asStateFlow()
    val commentsStateLive: LiveData<UiState<List<PostComment>>> = _commentsState.asLiveData()

    private val _commentPostState = MutableStateFlow(UiState<Unit>())
    val commentPostState: StateFlow<UiState<Unit>> = _commentPostState.asStateFlow()
    val commentPostStateLive: LiveData<UiState<Unit>> = _commentPostState.asLiveData()

    private var quotePollJob: Job? = null

    fun startLiveQuotePolling() {
        quotePollJob?.cancel()
        quotePollJob = viewModelScope.launch {
            while (isActive) {
                refreshFeedQuotes()
                delay(32_000)
            }
        }
    }

    fun stopLiveQuotePolling() {
        quotePollJob?.cancel()
        quotePollJob = null
    }

    fun refreshFeedQuotes() {
        viewModelScope.launch {
            val posts = _feedState.value.data ?: return@launch
            if (posts.none { !it.stockSymbol.isNullOrBlank() }) return@launch
            val merged = feedRepository.refreshQuotesForPosts(posts)
            _feedState.value = UiState(
                isLoading = false,
                data = merged,
                errorMessage = _feedState.value.errorMessage
            )
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            val cached = feedRepository.getCachedPosts()
            _feedState.value = UiState(
                isLoading = true,
                data = cached.takeIf { it.isNotEmpty() }
            )
            when (val result = feedRepository.getFeedPosts()) {
                is RepositoryResult.Success -> {
                    _feedState.value = UiState(data = result.data)
                }
                is RepositoryResult.Error -> {
                    _feedState.value = UiState(
                        data = cached.takeIf { it.isNotEmpty() },
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun publishPost(content: String, imageUri: Uri?) {
        viewModelScope.launch {
            _publishError.value = null
            _isPublishing.value = true
            when (val result = feedRepository.publishPost(content, imageUri)) {
                is RepositoryResult.Success -> {
                    _postPublished.value = true
                    loadFeed()
                }
                is RepositoryResult.Error -> {
                    _publishError.value = result.message
                }
            }
            _isPublishing.value = false
        }
    }

    fun consumePostPublished() {
        _postPublished.value = false
    }

    fun consumePublishError() {
        _publishError.value = null
    }

    fun loadPostDetails(postId: String) {
        viewModelScope.launch {
            _postDetailsState.value = UiState(isLoading = true)
            when (val result = feedRepository.getPostById(postId)) {
                is RepositoryResult.Success -> _postDetailsState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _postDetailsState.value = UiState(errorMessage = result.message)
            }
        }
    }

    fun updatePost(postId: String, content: String, imageUri: Uri?) {
        viewModelScope.launch {
            _postActionState.value = UiState(isLoading = true)
            when (val result = feedRepository.updatePost(postId, content, imageUri)) {
                is RepositoryResult.Success -> {
                    _postActionState.value = UiState(data = Unit)
                    loadPostDetails(postId)
                    loadFeed()
                }
                is RepositoryResult.Error -> _postActionState.value = UiState(errorMessage = result.message)
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _postActionState.value = UiState(isLoading = true)
            when (val result = feedRepository.deletePost(postId)) {
                is RepositoryResult.Success -> {
                    _postActionState.value = UiState(data = Unit)
                    loadFeed()
                }
                is RepositoryResult.Error -> _postActionState.value = UiState(errorMessage = result.message)
            }
        }
    }

    fun consumePostActionState() {
        _postActionState.value = UiState()
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            _commentsState.value = UiState(isLoading = true)
            when (val result = feedRepository.getComments(postId)) {
                is RepositoryResult.Success -> {
                    _commentsState.value = UiState(data = result.data)
                }
                is RepositoryResult.Error -> {
                    _commentsState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun postComment(postId: String, text: String) {
        viewModelScope.launch {
            _commentPostState.value = UiState(isLoading = true)
            when (val result = feedRepository.addComment(postId, text)) {
                is RepositoryResult.Success -> {
                    _commentPostState.value = UiState(data = Unit)
                    loadComments(postId)
                    loadFeed()
                }
                is RepositoryResult.Error -> {
                    _commentPostState.value = UiState(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun consumeCommentPostState() {
        _commentPostState.value = UiState()
    }

    fun resetCommentsState() {
        _commentsState.value = UiState()
    }
}

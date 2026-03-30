package com.stocksocial.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Post
import com.stocksocial.model.User
import com.stocksocial.model.UserSuggestion
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow(UiState<User>())
    val profileState: StateFlow<UiState<User>> = _profileState.asStateFlow()
    val profileStateLive: LiveData<UiState<User>> = _profileState.asLiveData()

    private val _userPostsState = MutableStateFlow(UiState<List<Post>>())
    val userPostsState: StateFlow<UiState<List<Post>>> = _userPostsState.asStateFlow()
    val userPostsStateLive: LiveData<UiState<List<Post>>> = _userPostsState.asLiveData()

    private val _profileUpdateState = MutableStateFlow(UiState<User>())
    val profileUpdateState: StateFlow<UiState<User>> = _profileUpdateState.asStateFlow()
    val profileUpdateStateLive: LiveData<UiState<User>> = _profileUpdateState.asLiveData()

    private val _followState = MutableStateFlow(UiState<Unit>())
    val followState: StateFlow<UiState<Unit>> = _followState.asStateFlow()
    val followStateLive: LiveData<UiState<Unit>> = _followState.asLiveData()

    private val _likePostState = MutableStateFlow(UiState<Unit>())
    val likePostState: StateFlow<UiState<Unit>> = _likePostState.asStateFlow()
    val likePostStateLive: LiveData<UiState<Unit>> = _likePostState.asLiveData()
    private val _userSearchState = MutableStateFlow(UiState<List<UserSuggestion>>(data = emptyList()))
    val userSearchState: StateFlow<UiState<List<UserSuggestion>>> = _userSearchState.asStateFlow()
    val userSearchStateLive: LiveData<UiState<List<UserSuggestion>>> = _userSearchState.asLiveData()
    private var searchJob: Job? = null

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = UiState(isLoading = true)
            when (val result = profileRepository.getProfile()) {
                is RepositoryResult.Success -> {
                    _profileState.value = UiState(data = result.data)
                }
                is RepositoryResult.Error -> {
                    _profileState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun loadMyPosts() {
        viewModelScope.launch {
            _userPostsState.value = UiState(isLoading = true)
            when (val result = profileRepository.getMyPosts()) {
                is RepositoryResult.Success -> {
                    _userPostsState.value = UiState(data = result.data)
                }
                is RepositoryResult.Error -> {
                    _userPostsState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun updateProfile(newName: String?, newImageUri: Uri?) {
        viewModelScope.launch {
            _profileUpdateState.value = UiState(isLoading = true)
            when (val result = profileRepository.updateProfile(newName, newImageUri)) {
                is RepositoryResult.Success -> {
                    _profileUpdateState.value = UiState(data = result.data)
                    _profileState.value = UiState(data = result.data)
                }
                is RepositoryResult.Error -> {
                    _profileUpdateState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun consumeProfileUpdateState() {
        _profileUpdateState.value = UiState()
    }

    fun followUserByUsername(username: String) {
        viewModelScope.launch {
            _followState.value = UiState(isLoading = true)
            when (val result = profileRepository.followUserByUsername(username)) {
                is RepositoryResult.Success -> _followState.value = UiState(data = Unit)
                is RepositoryResult.Error -> _followState.value = UiState(errorMessage = result.message)
            }
        }
    }

    fun consumeFollowState() {
        _followState.value = UiState()
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            _likePostState.value = UiState(isLoading = true)
            when (val result = profileRepository.likePost(postId)) {
                is RepositoryResult.Success -> {
                    _likePostState.value = UiState(data = Unit)
                    loadMyPosts()
                }
                is RepositoryResult.Error -> {
                    _likePostState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun consumeLikePostState() {
        _likePostState.value = UiState()
    }

    fun searchUsersByPrefix(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            _userSearchState.value = UiState(data = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            _userSearchState.value = UiState(isLoading = true, data = _userSearchState.value.data)
            delay(220)
            when (val result = profileRepository.searchUsersByPrefix(q)) {
                is RepositoryResult.Success -> _userSearchState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _userSearchState.value = UiState(errorMessage = result.message, data = emptyList())
            }
        }
    }

    fun clearUserSearch() {
        _userSearchState.value = UiState(data = emptyList())
    }
}

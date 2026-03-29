package com.stocksocial.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Post
import com.stocksocial.model.User
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository? = null
) : ViewModel() {

    private val _profileState = MutableStateFlow(UiState<User>())
    val profileState: StateFlow<UiState<User>> = _profileState.asStateFlow()

    private val _userPostsState = MutableStateFlow(UiState<List<Post>>())
    val userPostsState: StateFlow<UiState<List<Post>>> = _userPostsState.asStateFlow()

    fun loadProfile() {
        val repository = profileRepository ?: run {
            _profileState.value = UiState(errorMessage = "ProfileRepository is not attached")
            return
        }

        viewModelScope.launch {
            _profileState.value = UiState(isLoading = true)
            when (val result = repository.getProfile()) {
                is RepositoryResult.Success -> _profileState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _profileState.value = UiState(errorMessage = result.message)
            }
        }
    }

    fun loadMyPosts() {
        val repository = profileRepository ?: run {
            _userPostsState.value = UiState(errorMessage = "ProfileRepository is not attached")
            return
        }

        viewModelScope.launch {
            _userPostsState.value = UiState(isLoading = true)
            when (val result = repository.getMyPosts()) {
                is RepositoryResult.Success -> _userPostsState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _userPostsState.value = UiState(errorMessage = result.message)
            }
        }
    }
}

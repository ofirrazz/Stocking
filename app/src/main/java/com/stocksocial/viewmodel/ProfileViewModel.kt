package com.stocksocial.viewmodel

import android.net.Uri
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
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow(UiState<User>())
    val profileState: StateFlow<UiState<User>> = _profileState.asStateFlow()

    private val _userPostsState = MutableStateFlow(UiState<List<Post>>())
    val userPostsState: StateFlow<UiState<List<Post>>> = _userPostsState.asStateFlow()

    private val _profileUpdateBusy = MutableStateFlow(false)
    val profileUpdateBusy: StateFlow<Boolean> = _profileUpdateBusy.asStateFlow()

    private val _profileUpdateError = MutableStateFlow<String?>(null)
    val profileUpdateError: StateFlow<String?> = _profileUpdateError.asStateFlow()

    private val _profileUpdated = MutableStateFlow(false)
    val profileUpdated: StateFlow<Boolean> = _profileUpdated.asStateFlow()

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

    fun updateProfile(displayName: String, photoUri: Uri?) {
        viewModelScope.launch {
            _profileUpdateError.value = null
            _profileUpdateBusy.value = true
            when (val result = profileRepository.updateProfile(displayName, photoUri)) {
                is RepositoryResult.Success -> {
                    _profileUpdated.value = true
                    loadProfile()
                }
                is RepositoryResult.Error -> {
                    _profileUpdateError.value = result.message
                }
            }
            _profileUpdateBusy.value = false
        }
    }

    fun consumeProfileUpdated() {
        _profileUpdated.value = false
    }

    fun consumeProfileUpdateError() {
        _profileUpdateError.value = null
    }
}

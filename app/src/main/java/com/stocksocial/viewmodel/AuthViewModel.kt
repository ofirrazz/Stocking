package com.stocksocial.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.User
import com.stocksocial.repository.AuthRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(UiState<AuthUiModel>())
    val authState: StateFlow<UiState<AuthUiModel>> = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState(isLoading = true)
            when (val result = authRepository.login(email, password)) {
                is RepositoryResult.Success -> {
                    _authState.value = UiState(
                        data = AuthUiModel(
                            isAuthenticated = true,
                            user = result.data
                        )
                    )
                }
                is RepositoryResult.Error -> {
                    _authState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState(isLoading = true)
            when (val result = authRepository.register(username, email, password)) {
                is RepositoryResult.Success -> {
                    _authState.value = UiState(
                        data = AuthUiModel(
                            isAuthenticated = true,
                            user = result.data
                        )
                    )
                }
                is RepositoryResult.Error -> {
                    _authState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _authState.value = UiState(
            data = AuthUiModel(isAuthenticated = false, user = null)
        )
    }
}

data class AuthUiModel(
    val isAuthenticated: Boolean,
    val user: User?
)

package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
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
    val authStateLive: LiveData<UiState<AuthUiModel>> = _authState.asLiveData()

    private val _resetPasswordState = MutableStateFlow(UiState<Unit>())
    val resetPasswordStateLive: LiveData<UiState<Unit>> = _resetPasswordState.asLiveData()

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

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = UiState(isLoading = true)
            when (val result = authRepository.signInWithGoogle(idToken)) {
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
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = UiState(
                data = AuthUiModel(isAuthenticated = false, user = null)
            )
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = UiState(isLoading = true)
            when (val result = authRepository.sendPasswordReset(email)) {
                is RepositoryResult.Success -> _resetPasswordState.value = UiState(data = Unit)
                is RepositoryResult.Error ->
                    _resetPasswordState.value = UiState(errorMessage = result.message)
            }
        }
    }

    fun consumeResetPasswordState() {
        _resetPasswordState.value = UiState()
    }

    /**
     * Clear any pending error/success so it isn't re-displayed after a configuration change
     * (e.g. rotation) or when the user re-enters the auth screen.
     */
    fun consumeAuthState() {
        val current = _authState.value
        if (current.errorMessage != null) {
            _authState.value = current.copy(errorMessage = null)
        }
    }
}

data class AuthUiModel(
    val isAuthenticated: Boolean,
    val user: User?
)

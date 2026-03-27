package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.stocksocial.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        _user.value = repository.currentUser
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            val result = repository.login(email, pass)
            if (result != null) {
                _user.value = result
            } else {
                _error.value = "Login failed"
            }
        }
    }

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            val result = repository.register(email, pass)
            if (result != null) {
                _user.value = result
            } else {
                _error.value = "Registration failed"
            }
        }
    }

    fun logout() {
        repository.logout()
        _user.value = null
    }
}

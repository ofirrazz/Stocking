package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.stocksocial.model.User
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    
    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _profile = MutableLiveData<User?>()
    val profile: LiveData<User?> = _profile

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        _user.value = auth.currentUser
    }

    fun loadProfile() {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            when (val result = profileRepository.getProfile(currentUser.uid)) {
                is RepositoryResult.Success -> _profile.value = result.data
                is RepositoryResult.Error -> _error.value = result.message
            }
        }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
    }

    fun clearError() {
        _error.value = null
    }
}

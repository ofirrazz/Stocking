package com.stocksocial.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.stocksocial.model.AppLocalDb
import com.stocksocial.model.Post
import com.stocksocial.repository.FeedRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: FeedRepository
    val allPosts: LiveData<List<Post>>
    
    // LiveData to track loading status
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    init {
        val postDao = AppLocalDb.getDatabase(application).postDao()
        repository = FeedRepository(postDao)
        allPosts = repository.allPosts
    }

    // Refresh posts and update loading state
    fun refresh() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.refreshPosts()
            _isLoading.postValue(false)
        }
    }

    fun addPost(post: Post) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.addPost(post)
            _isLoading.postValue(false)
        }
    }

    /**
     * Creates a Post from the currently logged-in user.
     * Keeps UI logic inside ViewModel (MVVM).
     */
    fun publishPost(content: String) {
        val user = auth.currentUser
        if (user == null) {
            _error.postValue("You must be logged in")
            return
        }

        val post = Post(
            id = UUID.randomUUID().toString(),
            authorId = user.uid,
            authorName = user.displayName ?: user.email ?: "Unknown",
            content = content,
            imageUrl = null,
            createdAt = System.currentTimeMillis()
        )
        addPost(post)
    }

    fun clearError() {
        _error.value = null
    }
}

package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Post
import com.stocksocial.repository.AuthRepository
import com.stocksocial.repository.LocalPostsRepository
import kotlinx.coroutines.launch

class UserPostsViewModel(
    private val localPostsRepository: LocalPostsRepository? = null,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _myPosts = MutableLiveData<List<Post>>(emptyList())
    val myPosts: LiveData<List<Post>> = _myPosts

    fun observeMyPosts(): LiveData<List<Post>> {
        val repository = localPostsRepository ?: return myPosts
        val userId = authRepository?.getCurrentUserId() ?: "local-user"
        return repository.observePostsByUser(userId)
    }

    fun createPost(content: String, imageUrl: String?) {
        val repository = localPostsRepository ?: return
        val userId = authRepository?.getCurrentUserId() ?: "local-user"
        val username = authRepository?.getCurrentUsername() ?: "StockSocial User"
        viewModelScope.launch {
            repository.createPost(
                userId = userId,
                username = username,
                content = content,
                imageUrl = imageUrl
            )
        }
    }

    fun deletePost(postId: String) {
        val repository = localPostsRepository ?: return
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }
}

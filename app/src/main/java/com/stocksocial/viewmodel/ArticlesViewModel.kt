package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Article
import com.stocksocial.repository.ArticleRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.launch

class ArticlesViewModel(
    private val repository: ArticleRepository = ArticleRepository()
) : ViewModel() {

    private val _articles = MutableLiveData<List<Article>>(emptyList())
    val articles: LiveData<List<Article>> = _articles

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadArticles() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = repository.getArticles()) {
                is RepositoryResult.Success -> _articles.value = result.data
                is RepositoryResult.Error -> _error.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}

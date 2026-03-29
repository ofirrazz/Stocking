package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Article
import com.stocksocial.repository.ArticlesRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArticlesViewModel(
    private val articlesRepository: ArticlesRepository
) : ViewModel() {

    private val _articlesState = MutableStateFlow(UiState<List<Article>>())
    val articlesState: StateFlow<UiState<List<Article>>> = _articlesState.asStateFlow()
    val articlesStateLive: LiveData<UiState<List<Article>>> = _articlesState.asLiveData()

    private val _articleDetailsState = MutableStateFlow(UiState<Article>())
    val articleDetailsState: StateFlow<UiState<Article>> = _articleDetailsState.asStateFlow()
    val articleDetailsStateLive: LiveData<UiState<Article>> = _articleDetailsState.asLiveData()

    private val _filteredArticlesState = MutableLiveData(UiState<List<Article>>())
    val filteredArticlesState: LiveData<UiState<List<Article>>> = _filteredArticlesState
    private var allArticles: List<Article> = emptyList()

    fun loadArticles() {
        viewModelScope.launch {
            _articlesState.value = UiState(isLoading = true)
            _filteredArticlesState.value = UiState(isLoading = true)
            when (val result = articlesRepository.getArticles()) {
                is RepositoryResult.Success -> {
                    _articlesState.value = UiState(data = result.data)
                    allArticles = result.data
                    applyCategoryFilter(null)
                }
                is RepositoryResult.Error -> {
                    _articlesState.value = UiState(errorMessage = result.message)
                    _filteredArticlesState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun loadArticleDetails(articleId: String) {
        viewModelScope.launch {
            _articleDetailsState.value = UiState(isLoading = true)
            when (val result = articlesRepository.getArticleById(articleId)) {
                is RepositoryResult.Success -> {
                    _articleDetailsState.value = UiState(data = result.data)
                }
                is RepositoryResult.Error -> {
                    _articleDetailsState.value = UiState(errorMessage = result.message)
                }
            }
        }
    }

    fun setCategoryFilter(category: String?) {
        applyCategoryFilter(category)
    }

    private fun applyCategoryFilter(category: String?) {
        val filtered = if (category.isNullOrBlank()) {
            allArticles
        } else {
            allArticles.filter { it.category.equals(category, ignoreCase = true) }
        }
        _filteredArticlesState.value = UiState(data = filtered)
    }
}

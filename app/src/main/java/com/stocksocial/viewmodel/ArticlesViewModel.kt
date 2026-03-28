package com.stocksocial.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.Article
import com.stocksocial.repository.ArticlesRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArticlesViewModel(
    private val articlesRepository: ArticlesRepository? = null
) : ViewModel() {

    private val _articlesState = MutableStateFlow(UiState<List<Article>>())
    val articlesState: StateFlow<UiState<List<Article>>> = _articlesState.asStateFlow()

    private val _articleDetailsState = MutableStateFlow(UiState<Article>())
    val articleDetailsState: StateFlow<UiState<Article>> = _articleDetailsState.asStateFlow()

    fun loadArticles() {
        val repository = articlesRepository ?: run {
            _articlesState.value = UiState(errorMessage = "ArticlesRepository is not attached")
            return
        }

        viewModelScope.launch {
            _articlesState.value = UiState(isLoading = true)
            when (val result = repository.getArticles()) {
                is RepositoryResult.Success -> _articlesState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _articlesState.value = UiState(errorMessage = result.message)
            }
        }
    }

    fun loadArticleDetails(articleId: String) {
        val repository = articlesRepository ?: run {
            _articleDetailsState.value = UiState(errorMessage = "ArticlesRepository is not attached")
            return
        }

        viewModelScope.launch {
            _articleDetailsState.value = UiState(isLoading = true)
            when (val result = repository.getArticleById(articleId)) {
                is RepositoryResult.Success -> _articleDetailsState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _articleDetailsState.value = UiState(errorMessage = result.message)
            }
        }
    }
}

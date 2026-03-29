package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
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

    private val _filteredArticlesState = MutableStateFlow(UiState<List<Article>>())
    val filteredArticlesState: StateFlow<UiState<List<Article>>> = _filteredArticlesState.asStateFlow()
    val filteredArticlesStateLive: LiveData<UiState<List<Article>>> = _filteredArticlesState.asLiveData()
    private var allArticles: List<Article> = emptyList()
    private var selectedCategory: String? = null

    fun loadArticles() {
        viewModelScope.launch {
            val cached = articlesRepository.getCachedArticles()
            allArticles = cached
            _articlesState.value = UiState(
                isLoading = true,
                data = cached.takeIf { it.isNotEmpty() }
            )
            publishFilteredState(isLoading = true, errorMessage = null)
            when (val result = articlesRepository.getArticles()) {
                is RepositoryResult.Success -> {
                    allArticles = result.data
                    _articlesState.value = UiState(data = result.data)
                    publishFilteredState(isLoading = false, errorMessage = null)
                }
                is RepositoryResult.Error -> {
                    _articlesState.value = UiState(
                        data = cached.takeIf { it.isNotEmpty() },
                        errorMessage = result.message
                    )
                    publishFilteredState(
                        isLoading = false,
                        errorMessage = result.message
                    )
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
        selectedCategory = category
        publishFilteredState()
    }

    private fun publishFilteredState(
        isLoading: Boolean = false,
        errorMessage: String? = null
    ) {
        val filtered = if (selectedCategory.isNullOrBlank()) {
            allArticles
        } else {
            allArticles.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
        _filteredArticlesState.value = UiState(
            isLoading = isLoading,
            data = filtered,
            errorMessage = errorMessage
        )
    }
}

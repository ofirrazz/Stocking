package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.PortfolioHolding
import com.stocksocial.model.SymbolSearchHit
import com.stocksocial.repository.PortfolioRepository
import com.stocksocial.repository.RepositoryResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PortfolioViewModel(
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _holdingsState = MutableStateFlow(UiState<List<PortfolioHolding>>(data = emptyList()))
    val holdingsState: StateFlow<UiState<List<PortfolioHolding>>> = _holdingsState.asStateFlow()
    val holdingsStateLive: LiveData<UiState<List<PortfolioHolding>>> = _holdingsState.asLiveData()

    private val _upsertState = MutableStateFlow(UiState<Unit>())
    val upsertState: StateFlow<UiState<Unit>> = _upsertState.asStateFlow()
    val upsertStateLive: LiveData<UiState<Unit>> = _upsertState.asLiveData()

    private val _symbolHints = MutableStateFlow<List<SymbolSearchHit>>(emptyList())
    val symbolHints: StateFlow<List<SymbolSearchHit>> = _symbolHints.asStateFlow()
    val symbolHintsLive: LiveData<List<SymbolSearchHit>> = _symbolHints.asLiveData()

    private var symbolSearchJob: Job? = null

    fun onPortfolioSymbolQuery(query: String) {
        symbolSearchJob?.cancel()
        val q = query.trim()
        if (q.length < 1) {
            _symbolHints.value = emptyList()
            return
        }
        symbolSearchJob = viewModelScope.launch {
            delay(280)
            when (val result = portfolioRepository.searchSymbolHints(q)) {
                is RepositoryResult.Success -> _symbolHints.value = result.data
                is RepositoryResult.Error -> _symbolHints.value = emptyList()
            }
        }
    }

    fun clearSymbolHints() {
        symbolSearchJob?.cancel()
        _symbolHints.value = emptyList()
    }

    fun loadHoldings() {
        viewModelScope.launch {
            _holdingsState.value = UiState(isLoading = true, data = _holdingsState.value.data)
            when (val result = portfolioRepository.getHoldings()) {
                is RepositoryResult.Success -> _holdingsState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _holdingsState.value = UiState(errorMessage = result.message, data = emptyList())
            }
        }
    }

    fun addOrUpdateHolding(symbol: String, buyPrice: Double, displayName: String? = null) {
        viewModelScope.launch {
            _upsertState.value = UiState(isLoading = true)
            when (val result = portfolioRepository.upsertHolding(symbol, buyPrice, displayName)) {
                is RepositoryResult.Success -> {
                    _upsertState.value = UiState(data = Unit)
                    loadHoldings()
                }
                is RepositoryResult.Error -> _upsertState.value = UiState(errorMessage = result.message)
            }
        }
    }

    fun consumeUpsertState() {
        _upsertState.value = UiState()
    }
}

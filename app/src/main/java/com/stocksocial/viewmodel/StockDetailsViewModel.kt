package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.AnalystRecommendation
import com.stocksocial.model.Post
import com.stocksocial.model.PriceChartSeries
import com.stocksocial.model.Stock
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.repository.StockDetailsFallback
import com.stocksocial.repository.StockDetailsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class StockDetailsViewModel(
    private val repository: StockDetailsRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _quoteState = MutableStateFlow(UiState<Stock>())
    val quoteState: StateFlow<UiState<Stock>> = _quoteState.asStateFlow()
    val quoteStateLive: LiveData<UiState<Stock>> = _quoteState.asLiveData()

    private val _recommendationsState = MutableStateFlow(UiState<List<AnalystRecommendation>>(data = emptyList()))
    val recommendationsState: StateFlow<UiState<List<AnalystRecommendation>>> = _recommendationsState.asStateFlow()
    val recommendationsStateLive: LiveData<UiState<List<AnalystRecommendation>>> = _recommendationsState.asLiveData()

    private val _postsState = MutableStateFlow(UiState<List<Post>>(data = emptyList()))
    val postsState: StateFlow<UiState<List<Post>>> = _postsState.asStateFlow()
    val postsStateLive: LiveData<UiState<List<Post>>> = _postsState.asLiveData()

    private val _chartState = MutableStateFlow(UiState<PriceChartSeries>(data = PriceChartSeries()))
    val chartState: StateFlow<UiState<PriceChartSeries>> = _chartState.asStateFlow()
    val chartStateLive: LiveData<UiState<PriceChartSeries>> = _chartState.asLiveData()

    private val _favoriteState = MutableStateFlow<Boolean?>(null)
    val favoriteStateLive: LiveData<Boolean?> = _favoriteState.asLiveData()

    private val _favoriteError = MutableStateFlow<String?>(null)
    val favoriteErrorLive: LiveData<String?> = _favoriteError.asLiveData()

    private var pollJob: Job? = null

    fun load(symbol: String) {
        pollJob?.cancel()
        val norm = symbol.trim().uppercase(Locale.US)
        pollJob = viewModelScope.launch {
            runInitialLoad(norm)
            while (isActive) {
                delay(30_000)
                refreshQuoteAndChart(norm)
            }
        }
    }

    private suspend fun runInitialLoad(symbol: String) {
        _quoteState.value = UiState(isLoading = true)
        _recommendationsState.value = UiState(isLoading = true, data = _recommendationsState.value.data)
        _postsState.value = UiState(isLoading = true, data = _postsState.value.data)
        _favoriteState.value = null

        coroutineScope {
            val quoteDeferred = async { repository.getLiveQuote(symbol) }
            val recDeferred = async { repository.getAnalystRecommendations(symbol) }
            val postsDeferred = async { repository.getPostsForSymbol(symbol) }
            val chartDeferred = async { repository.getPriceHistory(symbol) }
            val favDeferred = async { profileRepository.isSymbolFavorite(symbol) }

            val chartResult = chartDeferred.await()
            val series = when (chartResult) {
                is RepositoryResult.Success -> StockDetailsFallback.enrichChartIfThin(chartResult.data, symbol)
                is RepositoryResult.Error -> StockDetailsFallback.mockChart(symbol)
            }
            _chartState.value = UiState(data = series)

            when (val result = quoteDeferred.await()) {
                is RepositoryResult.Success -> {
                    val merged = result.data.copy(volume = series.lastVolume ?: result.data.volume)
                    _quoteState.value = UiState(data = merged)
                }
                is RepositoryResult.Error -> {
                    val fb = StockDetailsFallback.mockStock(symbol)
                    _quoteState.value = UiState(data = fb.copy(volume = series.lastVolume ?: fb.volume))
                }
            }
            when (val result = recDeferred.await()) {
                is RepositoryResult.Success -> {
                    val list = result.data.takeIf { it.isNotEmpty() } ?: StockDetailsFallback.mockRecommendations()
                    _recommendationsState.value = UiState(data = list)
                }
                is RepositoryResult.Error ->
                    _recommendationsState.value = UiState(data = StockDetailsFallback.mockRecommendations())
            }
            when (val result = postsDeferred.await()) {
                is RepositoryResult.Success -> _postsState.value = UiState(data = result.data)
                is RepositoryResult.Error -> _postsState.value = UiState(errorMessage = result.message, data = emptyList())
            }
            _favoriteState.value = favDeferred.await()
        }
    }

    private suspend fun refreshQuoteAndChart(symbol: String) {
        coroutineScope {
            val quoteDeferred = async { repository.getLiveQuote(symbol) }
            val chartDeferred = async { repository.getPriceHistory(symbol) }
            val chartResult = chartDeferred.await()
            val series = when (chartResult) {
                is RepositoryResult.Success -> StockDetailsFallback.enrichChartIfThin(chartResult.data, symbol)
                is RepositoryResult.Error ->
                    _chartState.value.data?.takeIf { it.points.size >= 2 }
                        ?: StockDetailsFallback.mockChart(symbol)
            }
            _chartState.value = UiState(data = series)

            when (val result = quoteDeferred.await()) {
                is RepositoryResult.Success -> {
                    val merged = result.data.copy(volume = series.lastVolume ?: result.data.volume)
                    _quoteState.value = UiState(data = merged)
                }
                is RepositoryResult.Error -> {
                    val cur = _quoteState.value.data
                    if (cur != null) {
                        _quoteState.value = UiState(data = cur.copy(volume = series.lastVolume ?: cur.volume))
                    } else {
                        val fb = StockDetailsFallback.mockStock(symbol)
                        _quoteState.value = UiState(data = fb.copy(volume = series.lastVolume ?: fb.volume))
                    }
                }
            }
        }
    }

    fun toggleFavorite(symbol: String) {
        viewModelScope.launch {
            val wantFavorite = _favoriteState.value != true
            when (val r = profileRepository.setSymbolFavorite(symbol, wantFavorite)) {
                is RepositoryResult.Success -> _favoriteState.value = wantFavorite
                is RepositoryResult.Error -> _favoriteError.value = r.message
            }
        }
    }

    fun consumeFavoriteError() {
        _favoriteError.value = null
    }
}

package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.model.PortfolioHolding
import com.stocksocial.model.Post
import com.stocksocial.model.PublicUserProfile
import com.stocksocial.model.WatchlistItem
import com.stocksocial.repository.PortfolioRepository
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.repository.WatchlistRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val portfolioRepository: PortfolioRepository,
    private val watchlistRepository: WatchlistRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow(UiState<PublicUserProfile>())
    val profileState: StateFlow<UiState<PublicUserProfile>> = _profileState.asStateFlow()
    val profileStateLive: LiveData<UiState<PublicUserProfile>> = _profileState.asLiveData()

    private val _postsState = MutableStateFlow(UiState<List<Post>>(data = emptyList()))
    val postsStateLive: LiveData<UiState<List<Post>>> = _postsState.asLiveData()

    private val _portfolioState = MutableStateFlow(UiState<List<PortfolioHolding>>(data = emptyList()))
    val portfolioStateLive: LiveData<UiState<List<PortfolioHolding>>> = _portfolioState.asLiveData()

    private val _watchlistState = MutableStateFlow(UiState<List<WatchlistItem>>(data = emptyList()))
    val watchlistStateLive: LiveData<UiState<List<WatchlistItem>>> = _watchlistState.asLiveData()

    fun load(username: String) {
        viewModelScope.launch {
            _profileState.value = UiState(isLoading = true)
            _postsState.value = UiState(isLoading = true, data = emptyList())
            _portfolioState.value = UiState(isLoading = true, data = emptyList())
            _watchlistState.value = UiState(isLoading = true, data = emptyList())

            when (val prof = profileRepository.getPublicProfileByUsername(username)) {
                is RepositoryResult.Error -> {
                    _profileState.value = UiState(errorMessage = prof.message)
                    _postsState.value = UiState(errorMessage = prof.message, data = emptyList())
                    _portfolioState.value = UiState(data = emptyList())
                    _watchlistState.value = UiState(data = emptyList())
                }
                is RepositoryResult.Success -> {
                    val p = prof.data
                    _profileState.value = UiState(data = p)
                    coroutineScope {
                        val postsD = async { profileRepository.getPostsByUserId(p.userId) }
                        val portD = async { portfolioRepository.getHoldingsForUser(p.userId) }
                        val wlD = async { watchlistRepository.getWatchlist() }
                        when (val posts = postsD.await()) {
                            is RepositoryResult.Success -> _postsState.value = UiState(data = posts.data)
                            is RepositoryResult.Error -> _postsState.value =
                                UiState(errorMessage = posts.message, data = emptyList())
                        }
                        when (val port = portD.await()) {
                            is RepositoryResult.Success -> _portfolioState.value = UiState(data = port.data)
                            is RepositoryResult.Error -> _portfolioState.value =
                                UiState(errorMessage = port.message, data = emptyList())
                        }
                        when (val wl = wlD.await()) {
                            is RepositoryResult.Success -> _watchlistState.value = UiState(data = wl.data)
                            is RepositoryResult.Error -> _watchlistState.value =
                                UiState(errorMessage = wl.message, data = emptyList())
                        }
                    }
                }
            }
        }
    }

    fun portfolioPerformancePercent(holdings: List<PortfolioHolding>): Double {
        val invested = holdings.sumOf { it.investedValue }
        val value = holdings.sumOf { it.currentValue }
        if (invested <= 0.0) return 0.0
        return ((value - invested) / invested) * 100.0
    }
}

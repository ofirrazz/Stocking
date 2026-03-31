package com.stocksocial.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.stocksocial.data.prefs.RecentHotSearchStore
import com.stocksocial.model.FeedHotStockCategory
import com.stocksocial.model.Stock
import com.stocksocial.model.StockSignal
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.repository.WatchlistRepository
import com.stocksocial.utils.DummyData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StocksViewModel(
    private val watchlistRepository: WatchlistRepository,
    private val profileRepository: ProfileRepository,
    private val recentHotSearchStore: RecentHotSearchStore
) : ViewModel() {

    private val _stocksState = MutableStateFlow(UiState(data = StocksUiData()))
    val stocksState: StateFlow<UiState<StocksUiData>> = _stocksState.asStateFlow()
    val stocksStateLive: LiveData<UiState<StocksUiData>> = _stocksState.asLiveData()

    private val _favoriteToggleError = MutableStateFlow<String?>(null)
    val favoriteToggleErrorLive: LiveData<String?> = _favoriteToggleError.asLiveData()

    private var hotSearchJob: Job? = null
    private var hotSearchRequestVersion: Int = 0

    fun loadStocks() {
        viewModelScope.launch {
            _stocksState.value = UiState(isLoading = true)

            val fallback = buildMockData()
            val recentSyms = recentHotSearchStore.getSymbolsOrdered()

            val favSymbolsResult = profileRepository.getFavoriteSymbolList()
            val favoriteSyms = when (favSymbolsResult) {
                is RepositoryResult.Success -> favSymbolsResult.data
                is RepositoryResult.Error -> emptyList()
            }

            val union = watchlistRepository.dashboardQuoteSymbolUnion(recentSyms, favoriteSyms)
            val mapResult = watchlistRepository.getQuotesMap(union)
            val quoteMap = when (mapResult) {
                is RepositoryResult.Success -> mapResult.data
                is RepositoryResult.Error -> emptyMap()
            }
            val quoteError = (mapResult as? RepositoryResult.Error)?.message

            fun pick(order: List<String>): List<Stock> =
                order.mapNotNull { sym -> quoteMap[sym.uppercase()] }

            val marketIndices = pick(WatchlistRepository.MARKET_SYMBOLS).takeIf { it.isNotEmpty() }
                ?: fallback.marketIndices
            val trendingStocks = pick(WatchlistRepository.TRENDING_SYMBOLS).takeIf { it.isNotEmpty() }
                ?: fallback.trendingStocks
            val watchlistStocks = pick(WatchlistRepository.WATCHLIST_SYMBOLS).takeIf { it.isNotEmpty() }
                ?: fallback.watchlist

            val hotTech = pick(FeedHotStockCategory.technology)
            val hotBanking = pick(FeedHotStockCategory.banking)
            val hotCrypto = pick(FeedHotStockCategory.crypto)

            val favoriteSymbols: Set<String> = when (favSymbolsResult) {
                is RepositoryResult.Success -> favSymbolsResult.data.map { it.uppercase() }.toSet()
                is RepositoryResult.Error -> emptySet()
            }

            val recentStocksOrdered = orderStocksBySymbolOrder(recentSyms, pick(recentSyms))
            val favoriteStocks = if (favoriteSyms.isEmpty()) {
                emptyList()
            } else {
                orderStocksBySymbolOrder(favoriteSyms, pick(favoriteSyms))
            }

            _stocksState.value = UiState(
                data = StocksUiData(
                    marketIndices = marketIndices,
                    trendingStocks = trendingStocks,
                    watchlist = watchlistStocks,
                    topSignals = fallback.topSignals,
                    recentSearches = recentStocksOrdered,
                    hotTechStocks = hotTech,
                    hotBankingStocks = hotBanking,
                    hotCryptoStocks = hotCrypto,
                    favoriteSymbols = favoriteSymbols,
                    favoriteStocks = favoriteStocks,
                    hotSearchResults = emptyList()
                ),
                errorMessage = quoteError
            )
        }
    }

    /**
     * Finnhub symbol search (debounced) so users can find tickers not on the fixed hot lists.
     */
    fun onTradingSearchQueryChanged(raw: String) {
        hotSearchJob?.cancel()
        val q = raw.trim()
        if (q.length < MIN_HOT_SEARCH_REMOTE_CHARS) {
            hotSearchRequestVersion++
            val d = _stocksState.value.data
            if (d != null && d.hotSearchResults.isNotEmpty()) {
                val cur = _stocksState.value
                _stocksState.value = cur.copy(data = d.copy(hotSearchResults = emptyList()))
            }
            return
        }
        val version = ++hotSearchRequestVersion
        hotSearchJob = viewModelScope.launch {
            delay(HOT_SEARCH_DEBOUNCE_MS)
            if (version != hotSearchRequestVersion) return@launch
            when (val r = watchlistRepository.searchSymbols(q)) {
                is RepositoryResult.Success -> {
                    if (version != hotSearchRequestVersion) return@launch
                    val hits = r.data.take(HOT_SEARCH_MAX_SYMBOLS)
                    val data = _stocksState.value.data ?: return@launch
                    if (hits.isEmpty()) {
                        _stocksState.value = UiState(data = data.copy(hotSearchResults = emptyList()))
                        return@launch
                    }
                    val syms = hits.map { it.symbol.uppercase() }
                    val quotes = when (val qr = watchlistRepository.getQuotesMap(syms)) {
                        is RepositoryResult.Success -> qr.data
                        else -> emptyMap()
                    }
                    val stocks = hits.mapNotNull { hit -> quotes[hit.symbol.uppercase()] }
                    if (version != hotSearchRequestVersion) return@launch
                    _stocksState.value = UiState(data = data.copy(hotSearchResults = stocks))
                }
                is RepositoryResult.Error -> {
                    if (version != hotSearchRequestVersion) return@launch
                    val data = _stocksState.value.data ?: return@launch
                    _stocksState.value = UiState(data = data.copy(hotSearchResults = emptyList()))
                }
            }
        }
    }

    fun refreshMarketSnapshot() {
        viewModelScope.launch {
            val prev = _stocksState.value.data ?: StocksUiData()
            val fallback = buildMockData(prev.watchlist.takeIf { it.isNotEmpty() })
            val mapResult = watchlistRepository.getQuotesMap(watchlistRepository.coreRefreshSymbolUnion())
            val m = when (mapResult) {
                is RepositoryResult.Success -> mapResult.data
                is RepositoryResult.Error -> emptyMap()
            }
            fun pick(order: List<String>) = order.mapNotNull { sym -> m[sym.uppercase()] }

            val marketIndices = pick(WatchlistRepository.MARKET_SYMBOLS).takeIf { it.isNotEmpty() }
                ?: prev.marketIndices.ifEmpty { fallback.marketIndices }
            val trendingStocks = pick(WatchlistRepository.TRENDING_SYMBOLS).takeIf { it.isNotEmpty() }
                ?: prev.trendingStocks.ifEmpty { fallback.trendingStocks }
            val watchlistStocks = pick(WatchlistRepository.WATCHLIST_SYMBOLS).takeIf { it.isNotEmpty() }
                ?: prev.watchlist.ifEmpty { fallback.watchlist }

            _stocksState.value = UiState(
                data = prev.copy(
                    marketIndices = marketIndices,
                    trendingStocks = trendingStocks,
                    watchlist = watchlistStocks
                )
            )
        }
    }

    fun loadMockStocks() {
        _stocksState.value = UiState(data = buildMockData())
    }

    fun searchStock(symbolQuery: String) {
        viewModelScope.launch {
            val currentData = _stocksState.value.data ?: StocksUiData()
            val q = symbolQuery.trim()
            if (q.isEmpty()) return@launch
            when (val result = watchlistRepository.getStockBySymbol(q)) {
                is RepositoryResult.Success -> {
                    val found = result.data
                    recentHotSearchStore.prependSymbol(found.symbol)
                    val syms = recentHotSearchStore.getSymbolsOrdered()
                    val recentList = when (val r = watchlistRepository.getQuotesMap(syms)) {
                        is RepositoryResult.Success ->
                            orderStocksBySymbolOrder(syms, syms.mapNotNull { sym -> r.data[sym.uppercase()] })
                        is RepositoryResult.Error -> currentData.recentSearches
                    }
                    _stocksState.value = UiState(
                        data = currentData.copy(
                            recentSearches = recentList,
                            hotSearchResults = emptyList()
                        )
                    )
                }
                is RepositoryResult.Error -> {
                    _stocksState.value = UiState(
                        data = currentData,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun toggleHotStockFavorite(stock: Stock) {
        viewModelScope.launch {
            val data = _stocksState.value.data ?: return@launch
            val sym = stock.symbol.trim().uppercase()
            val isFav = sym in data.favoriteSymbols
            val want = !isFav
            when (val r = profileRepository.setSymbolFavorite(sym, want)) {
                is RepositoryResult.Success -> {
                    val newSyms = data.favoriteSymbols.toMutableSet().apply {
                        if (want) add(sym) else remove(sym)
                    }
                    val newFavStocks = if (want) {
                        if (data.favoriteStocks.any { it.symbol.equals(sym, true) }) data.favoriteStocks
                        else data.favoriteStocks + stock
                    } else {
                        data.favoriteStocks.filterNot { it.symbol.equals(sym, true) }
                    }
                    _stocksState.value = UiState(
                        data = data.copy(
                            favoriteSymbols = newSyms,
                            favoriteStocks = newFavStocks
                        )
                    )
                }
                is RepositoryResult.Error -> {
                    _favoriteToggleError.value = r.message
                }
            }
        }
    }

    fun consumeFavoriteToggleError() {
        _favoriteToggleError.value = null
    }

    fun consumeError() {
        val cur = _stocksState.value
        _stocksState.value = cur.copy(errorMessage = null)
    }

    private fun buildMockData(watchlistOverride: List<Stock>? = null): StocksUiData {
        return StocksUiData(
            marketIndices = DummyData.marketIndices(),
            trendingStocks = DummyData.trendingStocks(),
            watchlist = watchlistOverride ?: DummyData.watchlistStocks(),
            topSignals = DummyData.topSignals(),
            recentSearches = emptyList(),
            hotSearchResults = emptyList()
        )
    }

    private fun orderStocksBySymbolOrder(order: List<String>, stocks: List<Stock>): List<Stock> {
        val map = stocks.associateBy { it.symbol.uppercase() }
        return order.mapNotNull { sym -> map[sym.uppercase()] }
    }

    companion object {
        private const val HOT_SEARCH_DEBOUNCE_MS = 380L
        private const val MIN_HOT_SEARCH_REMOTE_CHARS = 1
        private const val HOT_SEARCH_MAX_SYMBOLS = 15
    }
}

data class StocksUiData(
    val marketIndices: List<Stock> = emptyList(),
    val trendingStocks: List<Stock> = emptyList(),
    val watchlist: List<Stock> = emptyList(),
    val topSignals: List<StockSignal> = emptyList(),
    val recentSearches: List<Stock> = emptyList(),
    val hotTechStocks: List<Stock> = emptyList(),
    val hotBankingStocks: List<Stock> = emptyList(),
    val hotCryptoStocks: List<Stock> = emptyList(),
    val favoriteSymbols: Set<String> = emptySet(),
    val favoriteStocks: List<Stock> = emptyList(),
    /** Remote Finnhub search hits (quotes) while user types on Trading; not in fixed category lists. */
    val hotSearchResults: List<Stock> = emptyList()
)

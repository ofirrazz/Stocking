package com.stocksocial.ui.feed

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.stocksocial.R
import com.stocksocial.databinding.FragmentFeedBinding
import com.stocksocial.model.FeedHotStockCategory
import com.stocksocial.model.SearchSuggestion
import com.stocksocial.model.SearchSuggestionType
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.ui.adapters.FeedAdapter
import com.stocksocial.ui.adapters.FeedHotStocksAdapter
import com.stocksocial.ui.adapters.UnifiedSearchAdapter
import com.stocksocial.utils.appContainer
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.utils.focusAndShowKeyboard
import com.stocksocial.viewmodel.FeedViewModel
import com.stocksocial.viewmodel.ProfileViewModel
import com.stocksocial.viewmodel.StocksViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels { appViewModelFactory }
    private val stocksViewModel: StocksViewModel by viewModels { appViewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { appViewModelFactory }
    private val feedAdapter = FeedAdapter(
        onPostClick = { post ->
            val direction = FeedFragmentDirections.actionFeedFragmentToPostDetailsFragment(post.id)
            findNavController().navigate(direction)
        },
        onShareClick = { post -> sharePost(post) },
        onStockClick = { symbol ->
            val direction = FeedFragmentDirections.actionFeedFragmentToStockDetailsFragment(symbol)
            findNavController().navigate(direction)
        }
    )
    private val hotStocksAdapter = FeedHotStocksAdapter { symbol ->
        findNavController().navigate(FeedFragmentDirections.actionFeedFragmentToStockDetailsFragment(symbol))
    }
    private var lastShownError: String? = null
    private var searchJob: Job? = null
    private var marketRefreshJob: Job? = null
    private var feedHotChipId: Int = R.id.chipAll
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.hotStocksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.hotStocksRecyclerView.adapter = hotStocksAdapter

        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.feedRecyclerView.adapter = feedAdapter

        binding.searchPeopleButton.setOnClickListener { showSearchPeopleDialog() }
        binding.createPostButton.setOnClickListener {
            findNavController().navigate(FeedFragmentDirections.actionFeedFragmentToCreatePostFragment())
        }

        val chips = listOf(binding.chipAll, binding.chipTech, binding.chipBanking, binding.chipCrypto)
        fun applyChipStyle(selected: Chip) {
            chips.forEach { chip ->
                val on = chip.id == selected.id
                if (on) {
                    chip.setChipBackgroundColorResource(R.color.primary_gold)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                    chip.chipStrokeWidth = 0f
                } else {
                    chip.setChipBackgroundColorResource(R.color.chip_bg)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    chip.chipStrokeWidth = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        1f,
                        resources.displayMetrics
                    )
                    chip.setChipStrokeColorResource(R.color.border)
                }
            }
        }
        binding.feedChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = binding.root.findViewById<Chip>(id)
            applyChipStyle(chip)
            feedHotChipId = id
            when (id) {
                R.id.chipAll -> {
                    stocksViewModel.stocksStateLive.value?.data?.trendingStocks?.let {
                        hotStocksAdapter.submitOrdered(it)
                    }
                }
                R.id.chipTech -> loadFeedHotCategory(FeedHotStockCategory.technology)
                R.id.chipBanking -> loadFeedHotCategory(FeedHotStockCategory.banking)
                R.id.chipCrypto -> loadFeedHotCategory(FeedHotStockCategory.crypto)
            }
        }
        applyChipStyle(binding.chipAll)

        stocksViewModel.stocksStateLive.observe(viewLifecycleOwner) { state ->
            if (feedHotChipId == R.id.chipAll) {
                state.data?.trendingStocks?.let { hotStocksAdapter.submitOrdered(it) }
            }
        }
        stocksViewModel.loadStocks()

        viewModel.feedStateLive.observe(viewLifecycleOwner) { state ->
            binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.data?.let { posts ->
                feedAdapter.submitList(posts)
                binding.feedSectionSubtitle.text =
                    getString(R.string.feed_new_posts_count, posts.size)
            }
            val error = state.errorMessage
            if (!error.isNullOrBlank() && error != lastShownError) {
                lastShownError = error
                val text = if (!state.data.isNullOrEmpty()) {
                    getString(R.string.cached_data_note)
                } else {
                    error
                }
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.postPublishedLive.observe(viewLifecycleOwner) { published ->
            if (published) {
                Toast.makeText(requireContext(), R.string.shared_as_post, Toast.LENGTH_SHORT).show()
                viewModel.consumePostPublished()
            }
        }

        viewModel.publishErrorLive.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                viewModel.consumePublishError()
            }
        }

        profileViewModel.followStateLive.observe(viewLifecycleOwner) { state ->
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeFollowState()
            } else if (state.data != null) {
                Toast.makeText(requireContext(), R.string.follow_success, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeFollowState()
            }
        }

        viewModel.loadFeed()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startLiveQuotePolling()
        marketRefreshJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(45_000)
                stocksViewModel.refreshMarketSnapshot()
            }
        }
        stocksViewModel.refreshMarketSnapshot()
    }

    override fun onStop() {
        viewModel.stopLiveQuotePolling()
        marketRefreshJob?.cancel()
        marketRefreshJob = null
        super.onStop()
    }

    private fun loadFeedHotCategory(symbolsInOrder: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val r = appContainer.watchlistRepository.getStocksForSymbols(symbolsInOrder)) {
                is RepositoryResult.Success ->
                    hotStocksAdapter.submitInDisplayOrder(r.data, symbolsInOrder)
                is RepositoryResult.Error ->
                    Toast.makeText(requireContext(), r.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSearchPeopleDialog() {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_people_search, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val searchLayout = dialogView.findViewById<TextInputLayout>(R.id.searchInputLayout)
        val input = dialogView.findViewById<TextInputEditText>(R.id.searchInput)
        val resultsRecycler = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.searchResultsRecyclerView)
        val emptyText = dialogView.findViewById<android.widget.TextView>(R.id.emptyResultsText)
        val followButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.followButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)
        val adapter = UnifiedSearchAdapter { suggestion ->
            when (suggestion.type) {
                SearchSuggestionType.USER -> {
                    val username = suggestion.username ?: return@UnifiedSearchAdapter
                    val direction = FeedFragmentDirections.actionFeedFragmentToUserProfileFragment(username)
                    findNavController().navigate(direction)
                }
                SearchSuggestionType.STOCK -> {
                    val symbol = suggestion.symbol ?: return@UnifiedSearchAdapter
                    val direction = FeedFragmentDirections.actionFeedFragmentToStockDetailsFragment(symbol)
                    findNavController().navigate(direction)
                }
            }
            dialog.dismiss()
        }
        resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        resultsRecycler.adapter = adapter

        searchLayout.hint = getString(R.string.search_people_or_stock_hint)
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().trim()
                searchJob?.cancel()
                if (query.length < 1) {
                    adapter.submitList(emptyList())
                    emptyText.visibility = View.GONE
                    return
                }
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(220)
                    val userResults = when (val users = appContainer.profileRepository.searchUsersByPrefix(query)) {
                        is RepositoryResult.Success -> users.data.map {
                            SearchSuggestion(
                                title = "@${it.username}",
                                subtitle = "Open profile",
                                type = SearchSuggestionType.USER,
                                userId = it.id,
                                username = it.username
                            )
                        }
                        is RepositoryResult.Error -> emptyList()
                    }
                    val symbol = query.uppercase().removePrefix("$")
                    val stockResults = when (val stock = appContainer.watchlistRepository.getStockBySymbol(symbol)) {
                        is RepositoryResult.Success -> listOf(
                            SearchSuggestion(
                                title = stock.data.symbol,
                                subtitle = stock.data.name,
                                type = SearchSuggestionType.STOCK,
                                symbol = stock.data.symbol
                            )
                        )
                        is RepositoryResult.Error -> emptyList()
                    }
                    val merged = (stockResults + userResults).distinctBy { "${it.type}:${it.title}" }
                    adapter.submitList(merged)
                    emptyText.visibility = if (merged.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
        input.addTextChangedListener(watcher)
        searchLayout.setEndIconOnClickListener { }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                true
            } else {
                false
            }
        }
        followButton.visibility = View.GONE
        cancelButton.setOnClickListener { dialog.dismiss() }
        dialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            input.focusAndShowKeyboard()
        }
        dialog.setOnDismissListener {
            searchJob?.cancel()
            input.removeTextChangedListener(watcher)
        }
        dialog.show()
    }

    private fun sharePost(post: com.stocksocial.model.Post) {
        val postText = buildString {
            append("Shared from @${post.author.username}:\n")
            append(post.content)
            if (!post.stockSymbol.isNullOrBlank()) {
                append("\n\nTicker: ${post.stockSymbol}")
            }
            if (!post.imageUrl.isNullOrBlank()) {
                append("\nImage: ${post.imageUrl}")
            }
            if (!post.videoUrl.isNullOrBlank()) {
                append("\nVideo: ${post.videoUrl}")
            }
        }
        viewModel.publishPost(postText, null)
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadFeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

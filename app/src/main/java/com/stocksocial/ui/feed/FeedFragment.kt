package com.stocksocial.ui.feed

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.stocksocial.R
import com.stocksocial.databinding.FragmentFeedBinding
import com.stocksocial.model.Post
import com.stocksocial.model.SearchSuggestion
import com.stocksocial.model.SearchSuggestionType
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.repository.RepositoryResult
import com.stocksocial.ui.adapters.FeedAdapter
import com.stocksocial.ui.post.CommentsBottomSheet
import com.stocksocial.ui.adapters.UnifiedSearchAdapter
import com.stocksocial.utils.appContainer
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.utils.focusAndShowKeyboard
import com.stocksocial.viewmodel.FeedViewModel
import com.stocksocial.viewmodel.ProfileViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels { appViewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { appViewModelFactory }
    private val feedAdapter = FeedAdapter(
        onPostClick = { post ->
            val direction = FeedFragmentDirections.actionFeedFragmentToPostDetailsFragment(post.id)
            findNavController().navigate(direction)
        },
        onLikeClick = { post -> profileViewModel.likePost(post.id) },
        onCommentClick = { post -> openCommentsSheet(post.id) },
        onEditClick = { post ->
            val direction = FeedFragmentDirections.actionFeedFragmentToPostDetailsFragment(post.id)
            findNavController().navigate(direction)
        },
        onShareClick = { post -> showSharePostDialog(post) },
        onStockClick = { symbol ->
            val direction = FeedFragmentDirections.actionFeedFragmentToStockDetailsFragment(symbol)
            findNavController().navigate(direction)
        }
    )
    private var lastShownError: String? = null
    private var searchJob: Job? = null
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

        binding.feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.feedRecyclerView.adapter = feedAdapter

        setFragmentResultListener(CommentsBottomSheet.REQUEST_KEY) { _, bundle ->
            if (bundle.getBoolean(CommentsBottomSheet.EXTRA_UPDATED)) {
                viewModel.loadFeed()
            }
        }

        binding.searchPeopleButton.setOnClickListener { showSearchPeopleDialog() }
        binding.createPostButton.setOnClickListener {
            findNavController().navigate(FeedFragmentDirections.actionFeedFragmentToCreatePostFragment())
        }

        viewModel.feedStateLive.observe(viewLifecycleOwner) { state ->
            binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.data?.let { posts ->
                feedAdapter.currentUserId = FirebaseAuth.getInstance().currentUser?.uid
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

        profileViewModel.likePostStateLive.observe(viewLifecycleOwner) { state ->
            if (!state.errorMessage.isNullOrBlank()) {
                val msg = if (state.errorMessage == ProfileRepository.MESSAGE_ALREADY_LIKED) {
                    getString(R.string.already_liked_post)
                } else {
                    state.errorMessage
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeLikePostState()
            } else if (state.data != null) {
                Toast.makeText(requireContext(), R.string.post_liked, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeLikePostState()
                viewModel.loadFeed()
            }
        }

        viewModel.loadFeed()
    }

    override fun onStart() {
        super.onStart()
        viewModel.startLiveQuotePolling()
    }

    override fun onStop() {
        viewModel.stopLiveQuotePolling()
        super.onStop()
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

    private fun showSharePostDialog(post: Post) {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_share_post, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val captionInput = dialogView.findViewById<TextInputEditText>(R.id.sharePostCaptionInput)
        dialogView.findViewById<View>(R.id.sharePostCancelButton).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.sharePostPublishButton).setOnClickListener {
            val userMsg = captionInput.text?.toString()?.trim().orEmpty()
            val body = buildShareQuotedBody(post)
            val finalText = if (userMsg.isNotEmpty()) "$userMsg\n\n$body" else body
            viewModel.publishPost(finalText, null)
            dialog.dismiss()
        }
        dialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            captionInput.focusAndShowKeyboard()
        }
        dialog.show()
    }

    private fun openCommentsSheet(postId: String) {
        CommentsBottomSheet.newInstance(postId).show(childFragmentManager, "comments")
    }

    private fun buildShareQuotedBody(post: Post): String = buildString {
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

    override fun onResume() {
        super.onResume()
        viewModel.loadFeed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

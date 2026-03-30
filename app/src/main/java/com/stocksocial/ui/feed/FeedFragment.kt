package com.stocksocial.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.graphics.drawable.ColorDrawable
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stocksocial.R
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.databinding.FragmentFeedBinding
import com.stocksocial.ui.adapters.FeedAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.FeedViewModel
import com.stocksocial.viewmodel.ProfileViewModel

class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels { appViewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { appViewModelFactory }
    private val feedAdapter = FeedAdapter(
        onPostClick = { post ->
            val direction = FeedFragmentDirections.actionFeedFragmentToPostDetailsFragment(post.id)
            findNavController().navigate(direction)
        },
        onShareClick = { post ->
            sharePost(post)
        }
    )
    private var lastShownError: String? = null
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
        binding.searchPeopleButton.setOnClickListener { showSearchPeopleDialog() }

        viewModel.feedStateLive.observe(viewLifecycleOwner) { state ->
            binding.loadingProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.data?.let { posts -> feedAdapter.submitList(posts) }
            val error = state.errorMessage
            if (!error.isNullOrBlank() && error != lastShownError) {
                lastShownError = error
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
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

    private fun showSearchPeopleDialog() {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_people_search, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val searchLayout = dialogView.findViewById<TextInputLayout>(R.id.searchInputLayout)
        val input = dialogView.findViewById<TextInputEditText>(R.id.searchInput)
        val followButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.followButton)
        val cancelButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)

        searchLayout.hint = getString(R.string.search_people_hint)
        val performFollow = {
            val username = input.text?.toString()?.trim().orEmpty()
            if (username.isNotBlank()) {
                profileViewModel.followUserByUsername(username)
                dialog.dismiss()
            }
        }
        searchLayout.setEndIconOnClickListener { performFollow() }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performFollow()
                true
            } else {
                false
            }
        }
        followButton.setOnClickListener { performFollow() }
        cancelButton.setOnClickListener { dialog.dismiss() }
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

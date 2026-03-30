package com.stocksocial.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stocksocial.NavGraphDirections
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.FragmentProfileBinding
import com.stocksocial.ui.adapters.UserSearchAdapter
import com.stocksocial.ui.adapters.UserPostsAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.utils.focusAndShowKeyboard
import com.stocksocial.viewmodel.AuthViewModel
import com.stocksocial.viewmodel.FeedViewModel
import com.stocksocial.viewmodel.ProfileViewModel
import java.util.Locale

class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels { appViewModelFactory }
    private val feedViewModel: FeedViewModel by viewModels { appViewModelFactory }
    private val authViewModel: AuthViewModel by viewModels { appViewModelFactory }
    private val userPostsAdapter by lazy {
        UserPostsAdapter(
            onEditClick = { post ->
                val direction = ProfileFragmentDirections.actionProfileFragmentToPostDetailsFragment(post.id)
                findNavController().navigate(direction)
            },
            onLikeClick = { post ->
                profileViewModel.likePost(post.id)
            },
            onShareClick = { post ->
                sharePost(post)
            },
            onStockClick = { symbol ->
                val direction = ProfileFragmentDirections.actionProfileFragmentToStockDetailsFragment(symbol)
                findNavController().navigate(direction)
            }
        )
    }
    private var lastShownError: String? = null
    private var isProfileLoading = false
    private var isPostsLoading = false
    private var isProfileUpdating = false

    private val pickProfileImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            profileViewModel.updateProfile(newName = null, newImageUri = uri)
        }
    }

    private val pickVideoToShare = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            shareVideoUri(uri)
        }
    }

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.userPostsRecyclerView.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.userPostsRecyclerView.adapter = userPostsAdapter

        binding.logoutButton.setOnClickListener {
            authViewModel.logout()
            val direction = NavGraphDirections.actionGlobalLoginFragment()
            findNavController().navigate(direction)
        }

        binding.writePostButton.setOnClickListener {
            val direction = ProfileFragmentDirections.actionProfileFragmentToCreatePostFragment()
            findNavController().navigate(direction)
        }
        binding.shareStockButton.setOnClickListener { showShareStockDialog() }
        binding.uploadVideoButton.setOnClickListener { pickVideoToShare.launch("video/*") }
        binding.followUserButton.setOnClickListener { showFollowUserDialog() }
        binding.openPortfolioButton.setOnClickListener {
            val direction = ProfileFragmentDirections.actionProfileFragmentToPortfolioFragment()
            findNavController().navigate(direction)
        }

        binding.fullNameText.setOnClickListener { showEditNameDialog() }
        binding.profileImage.setOnClickListener { pickProfileImage.launch("image/*") }
        binding.uploadImageButton.setOnClickListener { pickProfileImage.launch("image/*") }

        profileViewModel.profileStateLive.observe(viewLifecycleOwner) { state ->
            isProfileLoading = state.isLoading
            updateLoading()
            val error = state.errorMessage
            if (!error.isNullOrBlank() && error != lastShownError) {
                lastShownError = error
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
            state.data?.let { user ->
                val display = user.displayName?.takeIf { it.isNotBlank() } ?: user.username
                binding.fullNameText.text = display
                binding.usernameText.text = "@${user.username}"
                binding.bioText.text = user.bio ?: ""
                binding.profileStatPostsValue.text = String.format(Locale.US, "%,d", user.postsCount)
                binding.profileStatFollowersValue.text = String.format(Locale.US, "%,d", user.followersCount)
                binding.profileStatLikesValue.text = String.format(Locale.US, "%,d", user.totalLikesReceived)
                val avatar = user.avatarUrl
                if (!avatar.isNullOrBlank()) {
                    Glide.with(binding.profileImage).load(avatar).circleCrop()
                        .placeholder(R.drawable.bg_profile_circle)
                        .into(binding.profileImage)
                } else {
                    binding.profileImage.setImageResource(android.R.drawable.ic_menu_myplaces)
                }
            }
        }

        profileViewModel.userPostsStateLive.observe(viewLifecycleOwner) { state ->
            isPostsLoading = state.isLoading
            updateLoading()
            val error = state.errorMessage
            if (!error.isNullOrBlank() && error != lastShownError) {
                lastShownError = error
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
            state.data?.let { posts -> userPostsAdapter.submitList(posts) }
        }

        profileViewModel.profileUpdateStateLive.observe(viewLifecycleOwner) { state ->
            isProfileUpdating = state.isLoading
            updateLoading()
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeProfileUpdateState()
            }
            state.data?.let {
                Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeProfileUpdateState()
            }
        }

        profileViewModel.followStateLive.observe(viewLifecycleOwner) { state ->
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeFollowState()
            } else if (state.data != null) {
                Toast.makeText(requireContext(), R.string.follow_success, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeFollowState()
                profileViewModel.loadProfile()
            }
        }

        profileViewModel.likePostStateLive.observe(viewLifecycleOwner) { state ->
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeLikePostState()
            } else if (state.data != null) {
                Toast.makeText(requireContext(), R.string.post_liked, Toast.LENGTH_SHORT).show()
                profileViewModel.consumeLikePostState()
                profileViewModel.loadProfile()
            }
        }

        feedViewModel.postPublishedLive.observe(viewLifecycleOwner) { published ->
            if (published) {
                Toast.makeText(requireContext(), R.string.shared_as_post, Toast.LENGTH_SHORT).show()
                feedViewModel.consumePostPublished()
                profileViewModel.loadMyPosts()
                profileViewModel.loadProfile()
            }
        }

        feedViewModel.publishErrorLive.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
                feedViewModel.consumePublishError()
            }
        }

        profileViewModel.loadProfile()
        profileViewModel.loadMyPosts()
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadMyPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateLoading() {
        binding.loadingProgress.visibility =
            if (isProfileLoading || isPostsLoading || isProfileUpdating) View.VISIBLE else View.GONE
    }

    private fun showEditNameDialog() {
        val input = EditText(requireContext()).apply {
            setText(binding.fullNameText.text?.toString().orEmpty())
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_profile_name)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                if (newName.isNotBlank()) {
                    profileViewModel.updateProfile(newName = newName, newImageUri = null)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener { input.focusAndShowKeyboard() }
                dialog.show()
            }
    }

    private fun showFollowUserDialog() {
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
        val adapter = UserSearchAdapter { suggestion ->
            profileViewModel.followUserByUsername(suggestion.username)
            dialog.dismiss()
        }
        resultsRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        resultsRecycler.adapter = adapter

        searchLayout.hint = getString(R.string.search_people_hint)
        val performFollow = {
            val username = input.text?.toString()?.trim().orEmpty()
            if (username.isNotBlank()) {
                profileViewModel.followUserByUsername(username)
                dialog.dismiss()
            }
        }
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                profileViewModel.searchUsersByPrefix(s?.toString().orEmpty())
            }
        }
        input.addTextChangedListener(watcher)
        profileViewModel.userSearchStateLive.removeObservers(viewLifecycleOwner)
        profileViewModel.userSearchStateLive.observe(viewLifecycleOwner) { state ->
            val data = state.data.orEmpty()
            adapter.submitList(data)
            emptyText.visibility = if (!state.isLoading && data.isEmpty() && input.text?.isNotBlank() == true) View.VISIBLE else View.GONE
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
        dialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            input.focusAndShowKeyboard()
        }
        dialog.setOnDismissListener {
            input.removeTextChangedListener(watcher)
            profileViewModel.userSearchStateLive.removeObservers(viewLifecycleOwner)
            profileViewModel.clearUserSearch()
        }
        dialog.show()
    }

    private fun showShareStockDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.stock_symbol_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.share_stock)
            .setView(input)
            .setPositiveButton(R.string.share) { _, _ ->
                val symbol = input.text?.toString()?.trim().orEmpty().uppercase()
                if (symbol.isBlank()) return@setPositiveButton
                val postText = getString(R.string.share_stock_text_template, symbol)
                feedViewModel.publishPost(postText, null)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener { input.focusAndShowKeyboard() }
                dialog.show()
            }
    }

    private fun shareVideoUri(uri: Uri) {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.upload_video)))
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
        feedViewModel.publishPost(postText, null)
    }
}

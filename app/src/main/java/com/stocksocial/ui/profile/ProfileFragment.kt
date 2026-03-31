package com.stocksocial.ui.profile

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.textfield.TextInputEditText
import com.stocksocial.NavGraphDirections
import com.stocksocial.R
import com.stocksocial.databinding.FragmentProfileBinding
import com.stocksocial.model.Post
import com.stocksocial.repository.ProfileRepository
import com.stocksocial.ui.adapters.UserPostsAdapter
import com.stocksocial.ui.post.CommentsBottomSheet
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
            onPostClick = { post ->
                val direction = ProfileFragmentDirections.actionProfileFragmentToPostDetailsFragment(post.id)
                findNavController().navigate(direction)
            },
            onLikeClick = { post -> profileViewModel.likePost(post.id) },
            onCommentClick = { post -> openCommentsSheet(post.id) },
            onEditClick = { post ->
                val direction = ProfileFragmentDirections.actionProfileFragmentToPostDetailsFragment(post.id)
                findNavController().navigate(direction)
            },
            onShareClick = { post -> showSharePostDialog(post) },
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

    private var editProfileAvatarView: ImageView? = null
    private var pendingEditImageUri: Uri? = null

    private val pickEditProfileImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            pendingEditImageUri = uri
            uri?.let { u ->
                editProfileAvatarView?.let { iv ->
                    Glide.with(this).load(u).circleCrop().into(iv)
                }
            }
        }

    private val pickPhotoOrVideo = registerForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let { showMediaPostDialog(it) }
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

        setFragmentResultListener(CommentsBottomSheet.REQUEST_KEY) { _, bundle ->
            if (bundle.getBoolean(CommentsBottomSheet.EXTRA_UPDATED)) {
                profileViewModel.loadMyPosts()
            }
        }

        binding.logoutButton.setOnClickListener {
            authViewModel.logout()
            val direction = NavGraphDirections.actionGlobalLoginFragment()
            findNavController().navigate(direction)
        }

        binding.editProfileButton.setOnClickListener { showEditProfileDialog() }

        binding.writePostButton.setOnClickListener {
            val direction = ProfileFragmentDirections.actionProfileFragmentToCreatePostFragment()
            findNavController().navigate(direction)
        }
        binding.shareStockButton.setOnClickListener { showShareStockDialog() }
        binding.uploadMediaButton.setOnClickListener {
            pickPhotoOrVideo.launch(
                PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)
            )
        }

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
            state.data?.let { posts ->
                userPostsAdapter.currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                userPostsAdapter.submitList(posts)
            }
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
                profileViewModel.loadProfile()
                profileViewModel.loadMyPosts()
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

    private fun openCommentsSheet(postId: String) {
        CommentsBottomSheet.newInstance(postId).show(childFragmentManager, "comments")
    }

    private fun showEditProfileDialog() {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val avatar = dialogView.findViewById<ImageView>(R.id.editProfileAvatar)
        editProfileAvatarView = avatar
        pendingEditImageUri = null

        profileViewModel.profileStateLive.value?.data?.let { user ->
            val url = user.avatarUrl
            if (!url.isNullOrBlank()) {
                Glide.with(this).load(url).circleCrop()
                    .placeholder(R.drawable.bg_profile_circle)
                    .into(avatar)
            } else {
                avatar.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
        } ?: avatar.setImageResource(android.R.drawable.ic_menu_myplaces)

        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.editDisplayNameInput)
        nameInput.setText(binding.fullNameText.text?.toString().orEmpty())

        dialogView.findViewById<View>(R.id.editProfilePickPhotoButton).setOnClickListener {
            pickEditProfileImage.launch("image/*")
        }
        dialogView.findViewById<View>(R.id.editProfileCancelButton).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.editProfileSaveButton).setOnClickListener {
            val trimmed = nameInput.text?.toString()?.trim().orEmpty()
            val nameToSend = trimmed.takeIf { it.isNotBlank() }
            if (nameToSend == null && pendingEditImageUri == null) {
                Toast.makeText(requireContext(), R.string.edit_profile_no_changes, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            profileViewModel.updateProfile(newName = nameToSend, newImageUri = pendingEditImageUri)
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            editProfileAvatarView = null
            pendingEditImageUri = null
        }
        dialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            nameInput.focusAndShowKeyboard()
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
            .also { d ->
                d.setOnShowListener { input.focusAndShowKeyboard() }
                d.show()
            }
    }

    private fun showMediaPostDialog(mediaUri: Uri) {
        val dialog = android.app.Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_media_post, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        val captionInput = dialogView.findViewById<TextInputEditText>(R.id.mediaPostCaptionInput)
        dialogView.findViewById<View>(R.id.mediaPostCancelButton).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.mediaPostPublishButton).setOnClickListener {
            val caption = captionInput.text?.toString()?.trim().orEmpty()
            feedViewModel.publishPost(caption, mediaUri)
            dialog.dismiss()
        }
        dialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            captionInput.focusAndShowKeyboard()
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
            feedViewModel.publishPost(finalText, null)
            dialog.dismiss()
        }
        dialog.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.94f).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            captionInput.focusAndShowKeyboard()
        }
        dialog.show()
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
}

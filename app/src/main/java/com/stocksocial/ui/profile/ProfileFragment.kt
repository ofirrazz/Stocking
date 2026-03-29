package com.stocksocial.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.FragmentProfileBinding
import com.stocksocial.ui.adapters.UserPostsAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.AuthViewModel
import com.stocksocial.viewmodel.FeedViewModel
import com.stocksocial.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels { appViewModelFactory }
    private val authViewModel: AuthViewModel by viewModels { appViewModelFactory }
    private val feedViewModel: FeedViewModel by viewModels { appViewModelFactory }

    private val userPostsAdapter = UserPostsAdapter(
        onEdit = { post ->
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToCreatePostFragment(post.id)
            )
        },
        onDelete = { post ->
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.confirm_delete_post)
                .setPositiveButton(R.string.yes) { _, _ ->
                    feedViewModel.deletePost(post.id)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    )

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
        binding.userPostsRecyclerView.isNestedScrollingEnabled = false

        binding.profileSettingsButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.profileShareButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.logoutButton.setOnClickListener {
            authViewModel.logout()
            val nav = findNavController()
            val opts = NavOptions.Builder()
                .setPopUpTo(nav.graph.startDestinationId, true)
                .setLaunchSingleTop(true)
                .build()
            nav.navigate(R.id.loginFragment, null, opts)
        }

        binding.writePostButton.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToCreatePostFragment(null)
            )
        }

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(
                ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment()
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    profileViewModel.profileState.collect { state ->
                        state.data?.let { user ->
                            binding.fullNameText.text = user.username
                            binding.usernameText.text = "@${user.username}"
                            binding.bioText.text = user.bio ?: ""
                            val avatar = user.avatarUrl
                            if (!avatar.isNullOrBlank()) {
                                Glide.with(binding.profileImage).load(avatar)
                                    .into(binding.profileImage)
                            }
                        }
                    }
                }
                launch {
                    profileViewModel.userPostsState.collect { state ->
                        state.data?.let { posts ->
                            userPostsAdapter.submitList(posts)
                            binding.statPostsValue.text = posts.size.toString()
                        }
                    }
                }
                launch {
                    feedViewModel.postDeletedId.collect { id ->
                        if (id != null) {
                            Toast.makeText(requireContext(), R.string.delete_post, Toast.LENGTH_SHORT).show()
                            feedViewModel.consumePostDeleted()
                            profileViewModel.loadMyPosts()
                        }
                    }
                }
                launch {
                    feedViewModel.publishError.collect { err ->
                        if (!err.isNullOrBlank()) {
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                            feedViewModel.consumePublishError()
                        }
                    }
                }
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
}

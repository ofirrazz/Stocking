package com.stocksocial.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stocksocial.NavGraphDirections
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.FragmentProfileBinding
import com.stocksocial.ui.adapters.UserPostsAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.AuthViewModel
import com.stocksocial.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels { appViewModelFactory }
    private val authViewModel: AuthViewModel by viewModels { appViewModelFactory }
    private val userPostsAdapter = UserPostsAdapter { post ->
        val direction = ProfileFragmentDirections.actionProfileFragmentToPostDetailsFragment(post.id)
        findNavController().navigate(direction)
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
                binding.fullNameText.text = user.username
                binding.usernameText.text = "@${user.username}"
                binding.bioText.text = user.bio ?: ""
                val avatar = user.avatarUrl
                if (!avatar.isNullOrBlank()) {
                    Glide.with(binding.profileImage).load(avatar).circleCrop()
                        .into(binding.profileImage)
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
            .show()
    }
}

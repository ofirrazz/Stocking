package com.stocksocial.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.stocksocial.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels { appViewModelFactory }
    private val authViewModel: AuthViewModel by viewModels { appViewModelFactory }
    private val userPostsAdapter = UserPostsAdapter()

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
            val nav = findNavController()
            val opts = NavOptions.Builder()
                .setPopUpTo(nav.graph.startDestinationId, true)
                .setLaunchSingleTop(true)
                .build()
            nav.navigate(R.id.loginFragment, null, opts)
        }

        binding.writePostButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_createPostFragment)
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
                                Glide.with(binding.profileImage).load(avatar).circleCrop()
                                    .into(binding.profileImage)
                            }
                        }
                    }
                }
                launch {
                    profileViewModel.userPostsState.collect { state ->
                        state.data?.let { posts -> userPostsAdapter.submitList(posts) }
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

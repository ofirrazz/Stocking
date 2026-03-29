package com.stocksocial.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.R
import com.stocksocial.databinding.FragmentProfileBinding
import com.stocksocial.ui.adapters.UserPostsAdapter
import com.stocksocial.utils.appContainer
import com.stocksocial.viewmodel.AppViewModelFactory
import com.stocksocial.viewmodel.ProfileViewModel
import com.stocksocial.viewmodel.UserPostsViewModel

class ProfileFragment : Fragment() {

    private val profileViewModel: ProfileViewModel by viewModels {
        AppViewModelFactory(profileRepository = appContainer.profileRepository)
    }
    private val userPostsViewModel: UserPostsViewModel by viewModels {
        AppViewModelFactory(
            authRepository = appContainer.authRepository,
            localPostsRepository = appContainer.localPostsRepository
        )
    }
    private val userPostsAdapter = UserPostsAdapter { post ->
        userPostsViewModel.deletePost(post.id)
        Toast.makeText(requireContext(), getString(R.string.post_deleted), Toast.LENGTH_SHORT).show()
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

        binding.userPostsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.userPostsRecyclerView.adapter = userPostsAdapter

        userPostsViewModel.observeMyPosts().observe(viewLifecycleOwner) { posts ->
            userPostsAdapter.submitList(posts)
        }

        binding.writePostButton.setOnClickListener {
            findNavController().navigate(R.id.createPostFragment)
        }

        binding.logoutButton.setOnClickListener {
            appContainer.authRepository.logout()
            findNavController().navigate(R.id.loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

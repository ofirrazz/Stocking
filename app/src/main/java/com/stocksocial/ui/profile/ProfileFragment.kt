package com.stocksocial.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.stocksocial.databinding.FragmentProfileBinding
import com.stocksocial.ui.adapters.UserPostsAdapter
import com.stocksocial.utils.appContainer
import com.stocksocial.utils.DummyData
import com.stocksocial.viewmodel.AppViewModelFactory
import com.stocksocial.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels {
        AppViewModelFactory(profileRepository = appContainer.profileRepository)
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
        binding.userPostsRecyclerView.adapter = UserPostsAdapter(DummyData.userPosts())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

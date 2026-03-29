package com.stocksocial.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.stocksocial.databinding.FragmentCreatePostBinding
import com.stocksocial.utils.appContainer
import com.stocksocial.viewmodel.AppViewModelFactory
import com.stocksocial.viewmodel.UserPostsViewModel

class CreatePostFragment : Fragment() {

    private val viewModel: UserPostsViewModel by viewModels {
        AppViewModelFactory(
            authRepository = appContainer.authRepository,
            localPostsRepository = appContainer.localPostsRepository
        )
    }
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.publishPostButton.setOnClickListener {
            val content = binding.postContentInput.text?.toString()?.trim().orEmpty()
            val imageUrl = binding.imageUrlInput.text?.toString()?.trim().orEmpty()
            if (content.isEmpty()) return@setOnClickListener

            viewModel.createPost(
                content = content,
                imageUrl = imageUrl.ifBlank { null }
            )
            Toast.makeText(requireContext(), getString(com.stocksocial.R.string.post_published), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

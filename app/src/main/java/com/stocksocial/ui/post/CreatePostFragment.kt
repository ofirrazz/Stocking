package com.stocksocial.ui.post

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.stocksocial.R
import com.stocksocial.databinding.FragmentCreatePostBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.FeedViewModel
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels { appViewModelFactory }
    private var pickedUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        pickedUri = uri
        if (uri != null) {
            _binding?.imagePreview?.setImageURI(uri)
            _binding?.imagePreview?.visibility = View.VISIBLE
        }
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

        binding.pickImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.publishPostButton.setOnClickListener {
            val content = binding.postContentInput.text.toString().trim()
            if (content.isEmpty() && pickedUri == null) {
                Toast.makeText(requireContext(), R.string.post_needs_content, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.publishPost(content, pickedUri)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.postPublished.collect { published ->
                        if (published) {
                            Toast.makeText(requireContext(), R.string.published_ok, Toast.LENGTH_SHORT).show()
                            viewModel.consumePostPublished()
                            findNavController().navigateUp()
                        }
                    }
                }
                launch {
                    viewModel.publishError.collect { err ->
                        if (!err.isNullOrBlank()) {
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                            viewModel.consumePublishError()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

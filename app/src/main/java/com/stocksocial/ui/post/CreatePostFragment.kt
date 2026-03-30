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
import androidx.navigation.fragment.findNavController
import com.stocksocial.R
import com.stocksocial.databinding.FragmentCreatePostBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.FeedViewModel

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

        binding.backButton.setOnClickListener { findNavController().navigateUp() }

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

        viewModel.postPublishedLive.observe(viewLifecycleOwner) { published ->
            if (published) {
                Toast.makeText(requireContext(), R.string.published_ok, Toast.LENGTH_SHORT).show()
                viewModel.consumePostPublished()
                findNavController().navigateUp()
            }
        }

        viewModel.isPublishingLive.observe(viewLifecycleOwner) { publishing ->
            binding.publishProgress.visibility = if (publishing) View.VISIBLE else View.GONE
            binding.publishPostButton.isEnabled = !publishing
            binding.pickImageButton.isEnabled = !publishing
        }

        viewModel.publishErrorLive.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) {
                Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                viewModel.consumePublishError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

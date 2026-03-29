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
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.FragmentPostDetailsBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.FeedViewModel
import java.io.File

class PostDetailsFragment : Fragment() {

    private val args: PostDetailsFragmentArgs by navArgs()
    private val viewModel: FeedViewModel by viewModels { appViewModelFactory }
    private var selectedImageUri: Uri? = null
    private var pendingDelete = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
        if (uri != null) {
            _binding?.postImage?.visibility = View.VISIBLE
            _binding?.postImage?.setImageURI(uri)
        }
    }

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pickImageButton.setOnClickListener { pickImage.launch("image/*") }

        binding.updatePostButton.setOnClickListener {
            pendingDelete = false
            val content = binding.postContentInput.text?.toString()?.trim().orEmpty()
            viewModel.updatePost(args.postId, content, selectedImageUri)
        }

        binding.deletePostButton.setOnClickListener {
            pendingDelete = true
            viewModel.deletePost(args.postId)
        }

        viewModel.postDetailsStateLive.observe(viewLifecycleOwner) { state ->
            binding.actionProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.errorMessage?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
            state.data?.let { post ->
                binding.postMetaText.text = "${post.author.username} • ${post.createdAt}"
                binding.postContentInput.setText(post.content)
                if (selectedImageUri == null) {
                    when {
                        !post.localImagePath.isNullOrBlank() -> {
                            binding.postImage.visibility = View.VISIBLE
                            Glide.with(binding.postImage).load(File(post.localImagePath))
                                .centerCrop().into(binding.postImage)
                        }
                        !post.imageUrl.isNullOrBlank() -> {
                            binding.postImage.visibility = View.VISIBLE
                            Glide.with(binding.postImage).load(post.imageUrl)
                                .centerCrop().into(binding.postImage)
                        }
                        else -> {
                            binding.postImage.visibility = View.GONE
                            binding.postImage.setImageDrawable(null)
                        }
                    }
                }
            }
        }

        viewModel.postActionStateLive.observe(viewLifecycleOwner) { state ->
            binding.actionProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.updatePostButton.isEnabled = !state.isLoading
            binding.deletePostButton.isEnabled = !state.isLoading
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                viewModel.consumePostActionState()
                pendingDelete = false
            } else if (state.data != null) {
                Toast.makeText(
                    requireContext(),
                    if (pendingDelete) getString(R.string.post_deleted) else getString(R.string.post_updated),
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.consumePostActionState()
                if (pendingDelete && findNavController().previousBackStackEntry != null) {
                    findNavController().navigateUp()
                }
                pendingDelete = false
            }
        }

        viewModel.loadPostDetails(args.postId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

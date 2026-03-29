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
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.FragmentCreatePostBinding
import com.stocksocial.model.Post
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.FeedViewModel
import kotlinx.coroutines.launch
import java.io.File

class CreatePostFragment : Fragment() {

    private val args: CreatePostFragmentArgs by navArgs()
    private val viewModel: FeedViewModel by viewModels { appViewModelFactory }
    private var pickedUri: Uri? = null
    private var removeImage = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        pickedUri = uri
        removeImage = false
        if (uri != null) {
            _binding?.imagePreview?.visibility = View.VISIBLE
            _binding?.imagePreview?.setImageURI(uri)
            _binding?.removeImageButton?.visibility = View.VISIBLE
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

        val editId = args.postId
        if (!editId.isNullOrBlank()) {
            binding.createPostTitle.setText(R.string.edit_post)
            binding.publishPostButton.setText(R.string.save)
            viewModel.prepareEditPost(editId)
        }

        binding.pickImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.removeImageButton.setOnClickListener {
            pickedUri = null
            removeImage = true
            binding.imagePreview.visibility = View.GONE
            binding.imagePreview.setImageDrawable(null)
            binding.removeImageButton.visibility = View.GONE
        }

        binding.publishPostButton.setOnClickListener {
            val content = binding.postContentInput.text.toString().trim()
            val isEdit = !editId.isNullOrBlank()
            if (!isEdit && content.isEmpty() && pickedUri == null) {
                Toast.makeText(requireContext(), R.string.post_needs_content, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isEdit) {
                if (content.isEmpty() && pickedUri == null && !removeImage) {
                    Toast.makeText(requireContext(), R.string.post_needs_content, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewModel.updatePost(editId!!, content, pickedUri, removeImage)
            } else {
                viewModel.publishPost(content, pickedUri)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.postForEdit.collect { post ->
                        bindPostForEdit(post, editId)
                    }
                }
                launch {
                    viewModel.editPostLoadError.collect { err ->
                        if (!err.isNullOrBlank()) {
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                            viewModel.consumeEditPostLoadError()
                        }
                    }
                }
                launch {
                    viewModel.postPublished.collect { published ->
                        if (published) {
                            val msg = if (editId.isNullOrBlank()) {
                                getString(R.string.published_ok)
                            } else {
                                getString(R.string.save)
                            }
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            viewModel.consumePostPublished()
                            viewModel.consumePostForEdit()
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
                launch {
                    viewModel.postWriteBusy.collect { busy ->
                        binding.writeProgress.visibility = if (busy) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun bindPostForEdit(post: Post?, editId: String?) {
        if (post == null || editId.isNullOrBlank()) return
        if (post.id != editId) return
        binding.postContentInput.setText(post.content)
        removeImage = false
        pickedUri = null
        when {
            !post.localImagePath.isNullOrBlank() -> {
                binding.imagePreview.visibility = View.VISIBLE
                Glide.with(binding.imagePreview).load(File(post.localImagePath!!))
                    .centerCrop().into(binding.imagePreview)
                binding.removeImageButton.visibility = View.VISIBLE
            }
            !post.imageUrl.isNullOrBlank() -> {
                binding.imagePreview.visibility = View.VISIBLE
                Glide.with(binding.imagePreview).load(post.imageUrl).centerCrop()
                    .into(binding.imagePreview)
                binding.removeImageButton.visibility = View.VISIBLE
            }
            else -> {
                binding.imagePreview.visibility = View.GONE
                binding.removeImageButton.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.consumePostForEdit()
        _binding = null
    }
}

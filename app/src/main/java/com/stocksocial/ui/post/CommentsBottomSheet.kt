package com.stocksocial.ui.post

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.stocksocial.R
import com.stocksocial.databinding.BottomSheetCommentsBinding
import com.stocksocial.ui.adapters.CommentListAdapter
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.utils.focusAndShowKeyboard
import com.stocksocial.viewmodel.FeedViewModel

class CommentsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCommentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels(
        ownerProducer = { requireParentFragment() },
        factoryProducer = { appViewModelFactory }
    )

    private val postId: String by lazy {
        requireArguments().getString(ARG_POST_ID).orEmpty()
    }

    private val adapter = CommentListAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.commentsRecyclerView.adapter = adapter

        viewModel.loadComments(postId)

        viewModel.commentsStateLive.observe(viewLifecycleOwner) { state ->
            binding.commentsLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.data?.let { list ->
                adapter.submitList(list)
                binding.commentsEmptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
            if (!state.errorMessage.isNullOrBlank() && state.data == null) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.commentPostStateLive.observe(viewLifecycleOwner) { state ->
            binding.sendCommentButton.isEnabled = !state.isLoading
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_SHORT).show()
                viewModel.consumeCommentPostState()
            } else if (state.data != null) {
                Toast.makeText(requireContext(), R.string.comment_posted, Toast.LENGTH_SHORT).show()
                viewModel.consumeCommentPostState()
                binding.commentInput.text?.clear()
                parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(EXTRA_UPDATED to true))
            }
        }

        binding.sendCommentButton.setOnClickListener {
            val text = binding.commentInput.text?.toString().orEmpty()
            viewModel.postComment(postId, text)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.commentInput.focusAndShowKeyboard()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetCommentsState()
        _binding = null
    }

    companion object {
        const val ARG_POST_ID = "post_id"
        const val REQUEST_KEY = "comments_changed"
        const val EXTRA_UPDATED = "updated"

        fun newInstance(postId: String): CommentsBottomSheet =
            CommentsBottomSheet().apply {
                arguments = bundleOf(ARG_POST_ID to postId)
            }
    }
}

package com.stocksocial.ui.profile

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
import com.bumptech.glide.Glide
import com.stocksocial.R
import com.stocksocial.databinding.FragmentEditProfileBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels { appViewModelFactory }
    private var pickedUri: Uri? = null

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        pickedUri = uri
        if (uri != null) {
            _binding?.photoPreview?.visibility = View.VISIBLE
            _binding?.photoPreview?.setImageURI(uri)
        }
    }

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadProfile()

        binding.pickPhotoButton.setOnClickListener {
            pickPhoto.launch("image/*")
        }

        binding.saveProfileButton.setOnClickListener {
            val name = binding.nameInput.text?.toString().orEmpty()
            viewModel.updateProfile(name, pickedUri)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profileState.collect { state ->
                        state.data?.let { user ->
                            if (binding.nameInput.text.isNullOrBlank()) {
                                binding.nameInput.setText(user.username)
                            }
                            val avatar = user.avatarUrl
                            if (!avatar.isNullOrBlank() && pickedUri == null) {
                                Glide.with(binding.photoPreview).load(avatar).centerCrop()
                                    .into(binding.photoPreview)
                            }
                        }
                    }
                }
                launch {
                    viewModel.profileUpdated.collect { done ->
                        if (done) {
                            Toast.makeText(requireContext(), R.string.profile_updated, Toast.LENGTH_SHORT).show()
                            viewModel.consumeProfileUpdated()
                            findNavController().navigateUp()
                        }
                    }
                }
                launch {
                    viewModel.profileUpdateError.collect { err ->
                        if (!err.isNullOrBlank()) {
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                            viewModel.consumeProfileUpdateError()
                        }
                    }
                }
                launch {
                    viewModel.profileUpdateBusy.collect { busy ->
                        binding.saveProgress.visibility = if (busy) View.VISIBLE else View.GONE
                        binding.saveProfileButton.isEnabled = !busy
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

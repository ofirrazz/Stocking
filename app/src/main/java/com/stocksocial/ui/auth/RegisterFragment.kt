package com.stocksocial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.stocksocial.R
import com.stocksocial.databinding.FragmentRegisterBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels { appViewModelFactory }
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.registerToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.googleSignupButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }

        binding.goToLoginText.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    binding.registerButton.isEnabled = !state.isLoading
                    state.errorMessage?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                    if (state.data?.isAuthenticated == true &&
                        findNavController().currentDestination?.id == R.id.registerFragment
                    ) {
                        findNavController().navigate(R.id.action_registerFragment_to_feedFragment)
                    }
                }
            }
        }

        binding.registerButton.setOnClickListener {
            val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
            val email = binding.emailInput.text?.toString()?.trim().orEmpty()
            val password = binding.passwordInput.text?.toString().orEmpty()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), R.string.fill_register_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 8) {
                Toast.makeText(requireContext(), R.string.register_password_too_short, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.register(username, email, password)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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
import com.stocksocial.databinding.FragmentLoginBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels { appViewModelFactory }
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collect { state ->
                    binding.loginButton.isEnabled = !state.isLoading
                    state.errorMessage?.let { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                    val authenticated = state.data?.isAuthenticated == true
                    if (authenticated && findNavController().currentDestination?.id == R.id.loginFragment) {
                        findNavController().navigate(R.id.feedFragment)
                    }
                }
            }
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text?.toString()?.trim().orEmpty()
            val password = binding.passwordInput.text?.toString().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), R.string.fill_login_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.login(email, password)
        }

        binding.goToRegisterButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.stocksocial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.stocksocial.R
import com.stocksocial.databinding.FragmentLoginBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.utils.buildGoogleSignInClient
import com.stocksocial.utils.focusAndShowKeyboard
import com.stocksocial.viewmodel.AuthViewModel

class LoginFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels { appViewModelFactory }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (token.isNullOrBlank()) {
                Toast.makeText(requireContext(), R.string.google_signin_no_token, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }
            viewModel.signInWithGoogle(token)
        } catch (e: ApiException) {
            Toast.makeText(
                requireContext(),
                getString(R.string.google_signin_failed, e.message ?: e.statusCode.toString()),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
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
        binding.emailInput.focusAndShowKeyboard()

        viewModel.authStateLive.observe(viewLifecycleOwner) { state ->
            binding.loginButton.isEnabled = !state.isLoading
            binding.googleLoginButton.isEnabled = !state.isLoading
            binding.goToRegisterButton.isEnabled = !state.isLoading
            binding.loginProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.errorMessage?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
            val authenticated = state.data?.isAuthenticated == true
            if (authenticated && findNavController().currentDestination?.id == R.id.loginFragment) {
                val direction = LoginFragmentDirections.actionLoginFragmentToFeedFragment()
                findNavController().navigate(direction)
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
            val direction = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(direction)
        }

        binding.googleLoginButton.setOnClickListener {
            val client = buildGoogleSignInClient(requireContext()) ?: run {
                Toast.makeText(requireContext(), R.string.google_signin_web_client_missing, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

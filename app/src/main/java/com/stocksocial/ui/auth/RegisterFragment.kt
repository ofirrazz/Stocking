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
import com.stocksocial.databinding.FragmentRegisterBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.utils.buildGoogleSignInClient
import com.stocksocial.utils.focusAndShowKeyboard
import com.stocksocial.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

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
        binding.usernameInput.focusAndShowKeyboard()

        viewModel.authStateLive.observe(viewLifecycleOwner) { state ->
            binding.registerButton.isEnabled = !state.isLoading
            binding.googleRegisterButton.isEnabled = !state.isLoading
            binding.registerProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.errorMessage?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
            if (state.data?.isAuthenticated == true &&
                findNavController().currentDestination?.id == R.id.registerFragment
            ) {
                val direction = RegisterFragmentDirections.actionRegisterFragmentToFeedFragment()
                findNavController().navigate(direction)
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
            viewModel.register(username, email, password)
        }

        binding.googleRegisterButton.setOnClickListener {
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

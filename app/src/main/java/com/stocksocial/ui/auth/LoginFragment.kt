package com.stocksocial.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.stocksocial.R
import com.stocksocial.databinding.FragmentLoginBinding
import com.stocksocial.utils.appViewModelFactory
import com.stocksocial.utils.buildGoogleSignInClient
import com.stocksocial.utils.googleSignInErrorMessage
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
                googleSignInErrorMessage(requireContext(), e),
                Toast.LENGTH_LONG
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

        binding.backButton.setOnClickListener { navigateBackToWelcome() }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = navigateBackToWelcome()
            }
        )

        viewModel.authStateLive.observe(viewLifecycleOwner) { state ->
            binding.loginButton.isEnabled = !state.isLoading
            binding.googleLoginButton.isEnabled = !state.isLoading
            binding.goToRegisterButton.isEnabled = !state.isLoading
            binding.loginProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            state.errorMessage?.let { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                viewModel.consumeAuthState()
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

        binding.forgotPasswordText.setOnClickListener { showForgotPasswordDialog() }

        viewModel.resetPasswordStateLive.observe(viewLifecycleOwner) { state ->
            if (state.isLoading) return@observe
            if (!state.errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), state.errorMessage, Toast.LENGTH_LONG).show()
                viewModel.consumeResetPasswordState()
            } else if (state.data != null) {
                Toast.makeText(requireContext(), R.string.forgot_password_sent, Toast.LENGTH_LONG).show()
                viewModel.consumeResetPasswordState()
            }
        }
    }

    private fun showForgotPasswordDialog() {
        val context = requireContext()
        val container = TextInputLayout(context)
        val input = TextInputEditText(context).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = getString(R.string.forgot_password_email_hint)
            setText(binding.emailInput.text?.toString()?.trim().orEmpty())
        }
        container.addView(input)
        val padding = (resources.displayMetrics.density * 16).toInt()
        container.setPadding(padding, padding, padding, 0)
        AlertDialog.Builder(context)
            .setTitle(R.string.forgot_password_dialog_title)
            .setMessage(R.string.forgot_password_dialog_message)
            .setView(container)
            .setPositiveButton(R.string.forgot_password_send) { dialog, _ ->
                val email = (input as EditText).text?.toString()?.trim().orEmpty()
                if (email.isEmpty() ||
                    !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                ) {
                    Toast.makeText(context, R.string.forgot_password_invalid_email, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.sendPasswordReset(email)
                    dialog.dismiss()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun navigateBackToWelcome() {
        if (!findNavController().popBackStack(R.id.welcomeFragment, false)) {
            findNavController().navigate(R.id.welcomeFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

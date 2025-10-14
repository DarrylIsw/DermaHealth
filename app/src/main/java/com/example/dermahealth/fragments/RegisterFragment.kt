package com.example.dermahealth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.dermahealth.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

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

        // Back press handler
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        )

        binding.btnRegister.setOnClickListener {
            val fullName = binding.inputFullName.text.toString().trim()
            val username = binding.inputUsername.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            val confirm = binding.inputConfirmPassword.text.toString().trim()

            // Validate Full Name
            if (fullName.isEmpty()) {
                binding.inputFullName.error = "Full name is required"
                binding.inputFullName.requestFocus()
                return@setOnClickListener
            }

            if (!fullName.contains(" ")) {
                binding.inputFullName.error = "Please enter full name (first & last)"
                binding.inputFullName.requestFocus()
                return@setOnClickListener
            }

            // Validate Username
            if (username.isEmpty()) {
                binding.inputUsername.error = "Username is required"
                binding.inputUsername.requestFocus()
                return@setOnClickListener
            }

            if (username.length < 4) {
                binding.inputUsername.error = "Username must be at least 4 characters"
                binding.inputUsername.requestFocus()
                return@setOnClickListener
            }

            if (username.contains(" ")) {
                binding.inputUsername.error = "Username cannot contain spaces"
                binding.inputUsername.requestFocus()
                return@setOnClickListener
            }

            // Validate Email
            if (email.isEmpty()) {
                binding.inputEmail.error = "Email is required"
                binding.inputEmail.requestFocus()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.inputEmail.error = "Enter a valid email address"
                binding.inputEmail.requestFocus()
                return@setOnClickListener
            }

            // Validate Password
            if (password.isEmpty()) {
                binding.inputPassword.error = "Password is required"
                binding.inputPassword.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.inputPassword.error = "Password must be at least 6 characters"
                binding.inputPassword.requestFocus()
                return@setOnClickListener
            }

            if (!password.matches(".*[A-Z].*".toRegex())) {
                binding.inputPassword.error = "Password must contain an uppercase letter"
                binding.inputPassword.requestFocus()
                return@setOnClickListener
            }

            if (!password.matches(".*[0-9].*".toRegex())) {
                binding.inputPassword.error = "Password must contain a number"
                binding.inputPassword.requestFocus()
                return@setOnClickListener
            }

            // Validate Confirm Password
            if (confirm.isEmpty()) {
                binding.inputConfirmPassword.error = "Please confirm your password"
                binding.inputConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            if (password != confirm) {
                binding.inputConfirmPassword.error = "Passwords do not match"
                binding.inputConfirmPassword.requestFocus()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()

            // Navigate back to login
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.tvLoginLink.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.dermahealth

import android.os.Bundle
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

        // 🔙 Handle physical back press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Pop back to login fragment
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        )

        // 🔘 Register button logic
        binding.btnRegister.setOnClickListener {
            val fullName = binding.inputFullName.text.toString().trim()
            val username = binding.inputUsername.text.toString().trim()
            val email = binding.inputEmail.text.toString().trim()
            val password = binding.inputPassword.text.toString().trim()
            val confirm = binding.inputConfirmPassword.text.toString().trim()

            if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirm.isEmpty()
            ) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
            // Optional: Go back to login screen automatically after registration
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 🧭 Login link navigation
        binding.tvLoginLink.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

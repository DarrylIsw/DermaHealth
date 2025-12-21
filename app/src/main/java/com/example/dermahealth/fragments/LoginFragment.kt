package com.example.dermahealth.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.dermahealth.MainActivity
import com.example.dermahealth.R
import com.example.dermahealth.SessionManager
import com.example.dermahealth.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnLogin.setOnClickListener {
            handleLogin()
        }

        // Navigation wrapped with runIfSafe
        val goToRegister = {
            runIfSafe {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, RegisterFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.btnRegister.setOnClickListener { goToRegister() }
        binding.tvRegisterLink.setOnClickListener { goToRegister() }
    }

    private fun handleLogin() {
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        // --- Email Validation ---
        if (email.isEmpty()) {
            binding.inputEmail.error = "Email is required"
            binding.inputEmail.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.inputEmail.error = "Enter a valid email address"
            binding.inputEmail.requestFocus()
            return
        }

        // --- Password Validation ---
        if (password.isEmpty()) {
            binding.inputPassword.error = "Password is required"
            binding.inputPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            binding.inputPassword.error = "Password must be at least 6 characters"
            binding.inputPassword.requestFocus()
            return
        }

        // --- Firebase Sign-In ---
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                runIfSafe {
                    // SAVE LOGIN STATE
                    SessionManager.setLoggedIn(requireContext(), true)

                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
            .addOnFailureListener { e ->
                runIfSafe {
                    Toast.makeText(
                        requireContext(),
                        "Authentication failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    /**
     * Safe executor — prevents crashes like:
     * "Fragment not attached to a context" or "view == null"
     */
    private fun runIfSafe(block: () -> Unit) {
        if (isAdded && view != null && context != null) {
            block()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

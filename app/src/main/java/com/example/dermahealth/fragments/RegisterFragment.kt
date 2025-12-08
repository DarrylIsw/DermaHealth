package com.example.dermahealth.fragments

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.dermahealth.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Handle back press safely ---
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    runIfSafe {
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }
            }
        )

        // --- Register button ---
        binding.btnRegister.setOnClickListener {
            handleRegister()
        }

        // --- Login link ---
        binding.tvLoginLink.setOnClickListener {
            runIfSafe {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    private fun handleRegister() {
        val fullName = binding.inputFullName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val confirm = binding.inputConfirmPassword.text.toString().trim()
        val ageStr = binding.inputAge.text.toString().trim()

        // (Same validation logic, unchanged...)
        if (fullName.isEmpty()) {
            binding.inputFullName.error = "Full name is required"
            binding.inputFullName.requestFocus()
            return
        }
        if (!fullName.contains(" ")) {
            binding.inputFullName.error = "Please enter full name (first & last)"
            binding.inputFullName.requestFocus()
            return
        }
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
        if (!password.matches(".*[A-Z].*".toRegex())) {
            binding.inputPassword.error = "Password must contain an uppercase letter"
            binding.inputPassword.requestFocus()
            return
        }
        if (!password.matches(".*[0-9].*".toRegex())) {
            binding.inputPassword.error = "Password must contain a number"
            binding.inputPassword.requestFocus()
            return
        }
        if (confirm.isEmpty()) {
            binding.inputConfirmPassword.error = "Please confirm your password"
            binding.inputConfirmPassword.requestFocus()
            return
        }
        if (password != confirm) {
            binding.inputConfirmPassword.error = "Passwords do not match"
            binding.inputConfirmPassword.requestFocus()
            return
        }
        if (ageStr.isEmpty()) {
            binding.inputAge.error = "Age is required"
            binding.inputAge.requestFocus()
            return
        }
        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0) {
            binding.inputAge.error = "Enter a valid age"
            binding.inputAge.requestFocus()
            return
        }

        // --- Firebase Registration ---
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user!!.uid
                val db = FirebaseFirestore.getInstance()

                val userData = hashMapOf(
                    "fullName" to fullName,
                    "email" to email,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "age" to age.toString(),
                    "avatarUri" to ""
                )

                db.collection("users").document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        val statsData = hashMapOf(
                            "userId" to uid,
                            "totalScans" to 0,
                            "benignCount" to 0,
                            "neutralCount" to 0,
                            "suspiciousCount" to 0,
                            "malignantCount" to 0,
                            "overallSkinScore" to 0,
                            "lastUpdated" to FieldValue.serverTimestamp()
                        )

                        db.collection("statistics").document(uid)
                            .set(statsData)
                            .addOnSuccessListener {
                                runIfSafe {
                                    Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    requireActivity().supportFragmentManager.popBackStack()
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        runIfSafe {
                            Toast.makeText(requireContext(), "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                runIfSafe {
                    Toast.makeText(requireContext(), "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Safe execution wrapper
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

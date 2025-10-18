package com.example.dermahealth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

class LoginFragment : Fragment() {

    // --- Views ---
    private lateinit var inputUsername: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var tvRegisterLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the login layout
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Initialize views ---
        inputUsername = view.findViewById(R.id.inputUsername)
        inputPassword = view.findViewById(R.id.inputPassword)
        btnLogin = view.findViewById(R.id.btnLogin)
        btnRegister = view.findViewById(R.id.btnRegister)
        tvRegisterLink = view.findViewById(R.id.tvRegisterLink)

        // --- Login button click ---
        btnLogin.setOnClickListener {
            val username = inputUsername.text.toString().trim()
            val password = inputPassword.text.toString().trim()

            // --- Validate username ---
            if (username.isEmpty()) {
                inputUsername.error = "Username is required"
                inputUsername.requestFocus()
                return@setOnClickListener
            }

            if (username.contains(" ")) {
                inputUsername.error = "Username cannot contain spaces"
                inputUsername.requestFocus()
                return@setOnClickListener
            }

            // --- Validate password ---
            if (password.isEmpty()) {
                inputPassword.error = "Password is required"
                inputPassword.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                inputPassword.error = "Password must be at least 6 characters"
                inputPassword.requestFocus()
                return@setOnClickListener
            }

            // --- Dummy authentication (replace with Firebase/Auth later) ---
            if (username == "user" && password == "123456") {
                Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Navigate to RegisterFragment ---
        val goToRegister = {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        btnRegister.setOnClickListener { goToRegister() }
        tvRegisterLink.setOnClickListener { goToRegister() }
    }
}

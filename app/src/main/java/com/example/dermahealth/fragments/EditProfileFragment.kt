package com.example.dermahealth.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.dermahealth.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileFragment : Fragment() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAge: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: LinearLayout
    private lateinit var spinnerCountryCode: Spinner

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        // === Initialize UI elements ===
        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        etAge = view.findViewById(R.id.et_age)
        btnSave = view.findViewById(R.id.btn_save)
        btnBack = view.findViewById(R.id.btn_back)
        spinnerCountryCode = view.findViewById(R.id.spinner_country_code)

        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)

        // Load spinner data
        val savedCountryIndex = sharedPref.getInt("countryCodeIndex", 3) // Default: +62 (ID)
        spinnerCountryCode.setSelection(savedCountryIndex)

        // === Save button logic ===
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val age = etAge.text.toString().trim()
            val countryCode = spinnerCountryCode.selectedItem.toString()
            val selectedIndex = spinnerCountryCode.selectedItemPosition

            // === VALIDATION ===
            when {
                name.isEmpty() -> {
                    etName.error = "Full name cannot be empty"
                    etName.requestFocus()
                    return@setOnClickListener
                }
                !name.matches(Regex("^[A-Za-z ]+$")) -> {
                    etName.error = "Name can only contain letters and spaces"
                    etName.requestFocus()
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    etEmail.error = "Email cannot be empty"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etEmail.error = "Invalid email format"
                    etEmail.requestFocus()
                    return@setOnClickListener
                }
                phone.isEmpty() -> {
                    etPhone.error = "Phone number cannot be empty"
                    etPhone.requestFocus()
                    return@setOnClickListener
                }
                !phone.matches(Regex("^[0-9]{9,11}\$")) -> {
                    etPhone.error = "Enter a valid phone number (9–11 digits)"
                    etPhone.requestFocus()
                    return@setOnClickListener
                }
                age.isEmpty() -> {
                    etAge.error = "Age cannot be empty"
                    etAge.requestFocus()
                    return@setOnClickListener
                }
                !age.matches(Regex("^[0-9]{1,2}\$")) -> {
                    etAge.error = "Enter a valid age (1–99)"
                    etAge.requestFocus()
                    return@setOnClickListener
                }
                age.toInt() !in 1..99 -> {
                    etAge.error = "Age must be between 1 and 99"
                    etAge.requestFocus()
                    return@setOnClickListener
                }
                else -> {
                    // === Save locally ===
                    val editor = sharedPref.edit()
                    editor.putString("name", name)
                    editor.putString("email", email)
                    editor.putString("phone", "$countryCode $phone")
                    editor.putString("age", age)
                    editor.putInt("countryCodeIndex", selectedIndex)
                    editor.apply()

                    // === Save to Firebase ===
                    val currentUser = auth.currentUser
                    if (currentUser != null) {
                        val uid = currentUser.uid
                        val userData = hashMapOf(
                            "fullName" to name,
                            "email" to email,
                            "phone" to "$countryCode $phone",
                            "age" to age.toInt()
                        )

                        db.collection("users").document(uid)
                            .update(userData as Map<String, Any>)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener { e ->
                                Snackbar.make(view, "Failed to update profile: ${e.message}", Snackbar.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // === Back button ===
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle physical back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (parentFragmentManager.backStackEntryCount > 0) {
                    parentFragmentManager.popBackStack()
                } else {
                    requireActivity().finish()
                }
            }
        })
    }
}

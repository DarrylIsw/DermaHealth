package com.example.dermahealth.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.dermahealth.R
import androidx.activity.OnBackPressedCallback

class EditProfileFragment : Fragment() {

    // UI elements
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAge: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        // Initialize UI elements
        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        etAge = view.findViewById(R.id.et_age)
        btnSave = view.findViewById(R.id.btn_save)
        btnBack = view.findViewById(R.id.btn_back)

        // Load previously saved profile data from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        etName.setText(sharedPref.getString("name", ""))
        etEmail.setText(sharedPref.getString("email", ""))
        etPhone.setText(sharedPref.getString("phone", ""))
        etAge.setText(sharedPref.getString("age", ""))

        // Save button: store entered data in SharedPreferences
        btnSave.setOnClickListener {
            val editor = sharedPref.edit()
            editor.putString("name", etName.text.toString())
            editor.putString("email", etEmail.text.toString())
            editor.putString("phone", etPhone.text.toString())
            editor.putString("age", etAge.text.toString())
            editor.apply() // Save changes asynchronously

            parentFragmentManager.popBackStack() // Navigate back to previous fragment
        }

        // Back button in header: navigate back without saving
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Handle physical back button presses
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (parentFragmentManager.backStackEntryCount > 0) {
                    // Pop back stack if there are fragments
                    parentFragmentManager.popBackStack()
                } else {
                    // Finish activity if no fragments left
                    requireActivity().finish()
                }
            }
        })
    }
}


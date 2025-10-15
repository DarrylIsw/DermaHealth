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
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        etAge = view.findViewById(R.id.et_age)
        btnSave = view.findViewById(R.id.btn_save)
        btnBack = view.findViewById(R.id.btn_back)

        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        etName.setText(sharedPref.getString("name", ""))
        etEmail.setText(sharedPref.getString("email", ""))
        etPhone.setText(sharedPref.getString("phone", ""))
        etAge.setText(sharedPref.getString("age", ""))

        btnSave.setOnClickListener {
            val editor = sharedPref.edit()
            editor.putString("name", etName.text.toString())
            editor.putString("email", etEmail.text.toString())
            editor.putString("phone", etPhone.text.toString())
            editor.putString("age", etAge.text.toString())
            editor.apply()

            parentFragmentManager.popBackStack()
        }

        // Tombol back di header
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

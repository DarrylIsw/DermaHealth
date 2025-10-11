package com.example.dermahealth.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.dermahealth.R
import de.hdodenhof.circleimageview.CircleImageView
import com.example.dermahealth.helper.BackHandler

class ProfileFragment : Fragment(), BackHandler {
    override fun onBackPressed(): Boolean {
        // nothing special to handle → just return false
        return false
    }
    private lateinit var imgAvatar: CircleImageView
    private lateinit var btnEditAvatar: View
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvMobile: TextView
    private lateinit var tvAgeValue: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // 🔹 Bind views
        imgAvatar = view.findViewById(R.id.img_avatar)
        btnEditAvatar = view.findViewById(R.id.btn_edit_avatar)
        tvName = view.findViewById(R.id.tv_name)
        tvEmail = view.findViewById(R.id.tv_email)
        tvMobile = view.findViewById(R.id.tv_mobile)
        tvAgeValue = view.findViewById(R.id.tv_age_value)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account)

        // 🔹 Load saved user data
        loadUserData()

        // 🔹 Edit Profile button → buka EditProfileFragment
        btnEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        // 🔹 Logout button
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 🔹 Delete Account button
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("This action cannot be undone. Continue?")
                .setPositiveButton("Delete") { _, _ ->
                    val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 🔹 Edit Avatar button → buka ChangeAvatarFragment
        btnEditAvatar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChangeAvatarFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    // 🔹 Load user data from SharedPreferences
    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "John Doe")
        val email = sharedPref.getString("email", "john.doe@email.com")
        val phone = sharedPref.getString("phone", "+62 812-3456-7890")
        val age = sharedPref.getString("age", "24")

        tvName.text = name
        tvEmail.text = email
        tvMobile.text = phone
        tvAgeValue.text = age
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}

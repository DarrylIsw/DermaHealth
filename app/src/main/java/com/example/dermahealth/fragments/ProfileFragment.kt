package com.example.dermahealth.fragments

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
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
    private lateinit var tvTotalScans: TextView
    private lateinit var tvMemberSince: TextView
    private lateinit var tvProfile: TextView
    private lateinit var tvOverallSkin: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // 🔹 Bind views
        imgAvatar = view.findViewById(R.id.img_avatar_main)
        btnEditAvatar = view.findViewById(R.id.btn_edit_avatar_main)
        tvName = view.findViewById(R.id.tv_name)
        tvEmail = view.findViewById(R.id.tv_email)
        tvMobile = view.findViewById(R.id.tv_mobile)
        tvAgeValue = view.findViewById(R.id.tv_age_value)
        tvTotalScans = view.findViewById(R.id.tv_total_scans)
        tvMemberSince = view.findViewById(R.id.tv_member_since_date)
        tvOverallSkin = view.findViewById(R.id.tv_overall_skin_score)
        tvProfile = view.findViewById(R.id.tv_profile_title)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account)


        // 🔹 Load saved user data
        loadUserData()

        // 🔹 Edit Profile
        btnEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        // 🔹 Logout
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    val sharedPref =
                        requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                    // Reset UI
                    loadUserData()
                    imgAvatar.setImageResource(R.drawable.ic_person)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 🔹 Delete Account
        btnDeleteAccount.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("This action cannot be undone. Continue?")
                .setPositiveButton("Delete") { _, _ ->
                    val sharedPref =
                        requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    sharedPref.edit().clear().apply()
                    // Reset UI
                    loadUserData()
                    imgAvatar.setImageResource(R.drawable.ic_person)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 🔹 Edit Avatar
        btnEditAvatar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChangeAvatarFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    // 🔹 Load user data (nama, email, phone, age, avatar)
    private fun loadUserData() {
        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val name = sharedPref.getString("name", "John Doe")
        val email = sharedPref.getString("email", "john.doe@email.com")
        val phone = sharedPref.getString("phone", "+62 812-3456-7890")
        val age = sharedPref.getString("age", "24")
        val avatarUri = sharedPref.getString("avatarUri", null)

        tvName.text = name
        tvEmail.text = email
        tvMobile.text = phone
        tvAgeValue.text = age

        if (avatarUri != null) {
            try {
                val uri = Uri.parse(avatarUri)
                imgAvatar.setImageURI(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                imgAvatar.setImageResource(R.drawable.ic_person)
            }
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person)
        }
    }

    override fun onResume() {
        super.onResume()
        // Muat ulang data setiap kali kembali dari fragment lain
        loadUserData()
    }
}

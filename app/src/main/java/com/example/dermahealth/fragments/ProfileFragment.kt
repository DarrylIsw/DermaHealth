package com.example.dermahealth.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.dermahealth.R
import de.hdodenhof.circleimageview.CircleImageView

class ProfileFragment : Fragment() {

    private lateinit var imgAvatar: CircleImageView
    private lateinit var btnEditAvatar: View
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvMobile: TextView
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
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnDeleteAccount = view.findViewById(R.id.btn_delete_account)

        // 🔹 Edit Profile button
        btnEditProfile.setOnClickListener {
            // TODO: open edit screen or dialog
        }

        // 🔹 Logout button
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    // TODO: perform logout logic
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
                    // TODO: delete account logic
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 🔹 Avatar edit
        btnEditAvatar.setOnClickListener {
            // TODO: open image picker for avatar
        }

        return view
    }
}

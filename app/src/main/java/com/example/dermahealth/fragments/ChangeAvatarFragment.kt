package com.example.dermahealth.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.dermahealth.R
import com.google.android.material.imageview.ShapeableImageView

class ChangeAvatarFragment : Fragment() {

    // UI elements
    private lateinit var btnBack: View                 // Back button
    private lateinit var imgPreview: ShapeableImageView // Image preview for selected avatar
    private lateinit var btnChoose: Button            // Button to choose an image from gallery
    private lateinit var btnSave: Button              // Button to save selected avatar

    private var selectedImageUri: Uri? = null         // Stores the currently selected image URI

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_avatar, container, false)

        // Initialize UI elements
        btnBack = view.findViewById(R.id.btn_back)
        imgPreview = view.findViewById(R.id.img_preview)
        btnChoose = view.findViewById(R.id.btn_choose)
        btnSave = view.findViewById(R.id.btn_save)

        // --- Back button: returns to ProfileFragment ---
        btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileFragment())
                .commit()
        }

        // --- Choose image from gallery ---
        btnChoose.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"             // Only allow image files
            startActivityForResult(intent, 100) // Request code 100
        }

        // --- Save selected avatar to SharedPreferences ---
        btnSave.setOnClickListener {
            selectedImageUri?.let {
                val sharedPref = requireActivity()
                    .getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                sharedPref.edit().putString("avatarUri", it.toString()).apply() // Save URI as string

                // Return to ProfileFragment after saving
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ProfileFragment())
                    .commit()
            }
        }

        // --- Load previously saved avatar if exists ---
        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val savedUri = sharedPref.getString("avatarUri", null)
        if (savedUri != null) {
            try {
                imgPreview.setImageURI(Uri.parse(savedUri)) // Load saved URI
            } catch (e: Exception) {
                e.printStackTrace() // Handle invalid URI
            }
        }

        return view
    }

    // --- Handle result from gallery intent ---
    @Deprecated("Deprecated in Java") // This method is deprecated in favor of ActivityResult API
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data            // Get URI of selected image
            imgPreview.setImageURI(selectedImageUri) // Display selected image
        }
    }
}


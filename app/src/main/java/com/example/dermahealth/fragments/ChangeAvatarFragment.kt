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

    private lateinit var btnBack: View
    private lateinit var imgPreview: ShapeableImageView
    private lateinit var btnChoose: Button
    private lateinit var btnSave: Button

    private var selectedImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_avatar, container, false)

        btnBack = view.findViewById(R.id.btn_back)
        imgPreview = view.findViewById(R.id.img_preview)
        btnChoose = view.findViewById(R.id.btn_choose)
        btnSave = view.findViewById(R.id.btn_save)

        // Tombol back
        btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .commit()
        }

        // Pilih gambar dari galeri
        btnChoose.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        // Simpan avatar
        btnSave.setOnClickListener {
            selectedImageUri?.let {
                val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                sharedPref.edit().putString("avatarUri", it.toString()).apply()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ProfileFragment())
                    .commit()
            }
        }

        // Load avatar sebelumnya kalau ada
        val sharedPref = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val savedUri = sharedPref.getString("avatarUri", null)
        if (savedUri != null) {
            try {
                imgPreview.setImageURI(Uri.parse(savedUri))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            imgPreview.setImageURI(selectedImageUri)
        }
    }
}

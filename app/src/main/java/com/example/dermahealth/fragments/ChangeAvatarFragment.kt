package com.example.dermahealth.fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.dermahealth.R
import java.io.ByteArrayOutputStream
import androidx.activity.OnBackPressedCallback

class ChangeAvatarFragment : Fragment() {

    private lateinit var imgPreview: ImageView
    private lateinit var btnChoose: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var imageUri: Uri? = null

    // 📸 Ambil dari kamera
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                val uri = saveBitmapToCache(imageBitmap)
                imageUri = uri
                imgPreview.setImageURI(uri)
            }
        }

    // 🖼️ Pilih dari galeri
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                imgPreview.setImageURI(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_avatar, container, false)

        imgPreview = view.findViewById(R.id.img_preview)
        btnChoose = view.findViewById(R.id.btn_choose)
        btnSave = view.findViewById(R.id.btn_save)
        btnCancel = view.findViewById(R.id.btn_cancel)

        btnChoose.setOnClickListener {
            showChooseDialog()
        }

        btnSave.setOnClickListener {
            if (imageUri != null) {
                saveImageUri(imageUri!!)
                Toast.makeText(requireContext(), "Avatar updated!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "Please choose an image first", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        return view
    }

    // 🔹 Tampilkan dialog: pilih dari Kamera atau Galeri
    private fun showChooseDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Avatar Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(permission), 101)
        } else {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    // 🔹 Simpan URI ke SharedPreferences
    private fun saveImageUri(uri: Uri) {
        val prefs = requireActivity().getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        prefs.edit().putString("avatar_uri", uri.toString()).apply()
    }

    // 🔹 Konversi Bitmap → URI untuk preview kamera
    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            requireContext().contentResolver,
            bitmap,
            "temp_avatar",
            null
        )
        return Uri.parse(path)
    }
}

package com.example.dermahealth

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.OnBackPressedCallback
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.dermahealth.helper.BackHandler


class ScanFragment : Fragment(), BackHandler {
    override fun onBackPressed(): Boolean {
        // nothing special to handle → just return false
        return false
    }
    private lateinit var previewView: PreviewView
    private lateinit var btnTake: MaterialCardView
    private lateinit var btnGallery: MaterialCardView
    private lateinit var cardClassification: MaterialCardView
    private lateinit var tvClassResult: TextView
    private lateinit var btnSaveHistory: com.google.android.material.button.MaterialButton
    private lateinit var ivCropped: ImageView
    private lateinit var croppedCard: MaterialCardView

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    // Temp URIs
    private var lastSavedUri: Uri? = null
    private var lastCroppedUri: Uri? = null

    // Activity result launchers
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var pickGalleryLauncher: ActivityResultLauncher<String>
    private lateinit var uCropLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasReadPermission(): Boolean {
        val readPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES; but we'll request READ_EXTERNAL_STORAGE as fallback
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val res = ContextCompat.checkSelfPermission(requireContext(), readPerm)
        // If readPerm is not defined on older SDK, will evaluate to PERMISSION_DENIED - but app manifest should include legacy permission.
        return res == PackageManager.PERMISSION_GRANTED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        previewView = view.findViewById(R.id.previewView)
        btnTake = view.findViewById(R.id.btn_take_photo)
        btnGallery = view.findViewById(R.id.btn_pick_gallery)
        cardClassification = view.findViewById(R.id.card_classification)
        tvClassResult = view.findViewById(R.id.tv_class_result)
        btnSaveHistory = view.findViewById(R.id.btn_save_history)
        ivCropped = view.findViewById(R.id.iv_cropped)
        croppedCard = view.findViewById(R.id.card_cropped_preview)
        val infoCard = view.findViewById<MaterialCardView>(R.id.btn_info_dropdown)
        val infoContent = view.findViewById<LinearLayout>(R.id.info_content_container)
        val arrow = view.findViewById<ImageView>(R.id.iv_dropdown_arrow)

        // Permission launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback { results ->
                val granted = results.values.all { it }
                if (granted) {
                    startCamera()
                } else {
                    Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
                }
            }
        )

        // Gallery pick launcher (returns URI)
        pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { startCrop(uriToFileUri(uri)) } // convert content uri to file uri via copy -> uCrop needs file-based uri ideally
        }

        // uCrop launcher using startActivityForResult (UCrop uses its own activity)
        uCropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let { onCropped(it) }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(requireContext(), "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Click listeners
        btnTake.setOnClickListener {
            if (!hasCameraPermission()) {
                requestCameraAndMaybeStorage()
            } else {
                takePhotoAndCrop()
            }
        }

        btnGallery.setOnClickListener {
            // check read permission
            if (!hasReadPermission()) {
                requestReadPermissionThenGallery()
            } else {
                pickGalleryLauncher.launch("image/*")
            }
        }

        btnSaveHistory.setOnClickListener {
            lastCroppedUri?.let { uri ->
                // Save to history (dummy) — here we simply show Snackbar
                Snackbar.make(requireView(), "Scan saved successfully", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        // undo logic if needed
                    }.show()
                // In real app: persist to database/storage
            }
        }

        infoCard.setOnClickListener {
            if (infoContent.visibility == View.GONE) {
                // expand with fade-in
                infoContent.visibility = View.VISIBLE
                infoContent.alpha = 0f
                infoContent.animate().alpha(1f).setDuration(250).start()
                // rotate arrow
                arrow.animate().rotation(180f).setDuration(250).start()
            } else {
                // collapse
                infoContent.animate().alpha(0f).setDuration(250).withEndAction {
                    infoContent.visibility = View.GONE
                }.start()
                arrow.animate().rotation(0f).setDuration(250).start()
            }
        }

        // Start camera if permission already granted
        if (hasCameraPermission()) startCamera()
    }

    /** Convert content uri to a file: copy into cache and return file:// URI */
    private fun uriToFileUri(contentUri: Uri): Uri {
        // Create temp file in cache
        val inputStream = requireContext().contentResolver.openInputStream(contentUri)
        val tempFile = File(requireContext().cacheDir, "picked_${System.currentTimeMillis()}.jpg")
        inputStream.use { input ->
            tempFile.outputStream().use { out ->
                input?.copyTo(out)
            }
        }
        return Uri.fromFile(tempFile)
    }

    private fun requestCameraAndMaybeStorage() {
        val perms = mutableListOf<String>()
        perms.add(Manifest.permission.CAMERA)
        // for older devices maybe request read storage too
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(perms.toTypedArray())
    }

    private fun requestReadPermissionThenGallery() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pickGalleryLauncher.launch("image/*") else
                Toast.makeText(requireContext(), "Storage permission required to pick image", Toast.LENGTH_SHORT).show()
        }.launch(perm)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Camera start failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhotoAndCrop() {
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "DERMA_IMG_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        // Use external media store for persist or cache file for quick crop
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            requireContext().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(outputFileOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = output.savedUri
                lastSavedUri = savedUri
                // Convert to file Uri for cropping (uCrop works with file URIs)
                val uriToCrop = savedUri ?: return
                // launch crop
                requireActivity().runOnUiThread {
                    startCrop(uriToCrop)
                }
            }
        })
    }

    private fun startCrop(sourceUri: Uri) {
        // Destination
        val destFile = File(requireContext().cacheDir, "ucrop_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        lastCroppedUri = null

        // uCrop options: square default or free aspect
        val uCrop = UCrop.of(sourceUri, destUri)
            .withAspectRatio(1f, 1f) // enforce square crop for model input consistency
            .withMaxResultSize(1024, 1024)

        val intent = uCrop.getIntent(requireContext())
        uCropLauncher.launch(intent)
    }

    private fun onCropped(resultUri: Uri) {
        lastCroppedUri = resultUri
        // Show cropped preview
        croppedCard.visibility = View.VISIBLE
        ivCropped.setImageURI(resultUri)

        // Reveal classification card and save button
        cardClassification.visibility = View.VISIBLE
        btnSaveHistory.isEnabled = true

        // Dummy classification: random between Benign / Suspicious to show flow
        val rand = (0..1).random()
        val label = if (rand == 0) "Benign" else "Suspicious"
        tvClassResult.text = label

        // Smooth animation (scale in)
        croppedCard.scaleX = 0.7f
        croppedCard.scaleY = 0.7f
        croppedCard.alpha = 0f
        croppedCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300).start()

        // Scroll bottom container into view if needed (if using scroll)
    }
}

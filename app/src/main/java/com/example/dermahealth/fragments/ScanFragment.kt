package com.example.dermahealth.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.format.DateFormat
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.dermahealth.R
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.data.ScanImage
import com.example.dermahealth.helper.TFLiteClassifier
import com.example.dermahealth.viewmodel.SharedViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth


class ScanFragment : Fragment() {

    private lateinit var previewView: PreviewView
    private lateinit var btnTake: MaterialCardView
    private lateinit var btnGallery: MaterialCardView
    private lateinit var cardClassification: MaterialCardView
    private lateinit var tvClassResult: TextView
    private lateinit var tvClassInitial: TextView
    private lateinit var cardInitialCircle: MaterialCardView
    private lateinit var tvClassNote: TextView
    private lateinit var btnSaveHistory: MaterialButton
    private lateinit var ivCropped: ImageView
    private lateinit var croppedCard: MaterialCardView

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var btnInfoDropdown: MaterialCardView
    private lateinit var ivDropdownArrow: ImageView
    private lateinit var infoContentContainer: LinearLayout

    // temp values
    private var lastCroppedUri: Uri? = null
    private var lastClassificationLabel: String? = null
    private var lastClassificationScore: Float? = null

    private lateinit var pickGalleryLauncher: ActivityResultLauncher<String>
    private lateinit var uCropLauncher: ActivityResultLauncher<Intent>

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var tflite: TFLiteClassifier

    private val db = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        tflite = TFLiteClassifier(requireContext())
    }

    override fun onDestroy() {
        super.onDestroy()
        tflite.close()
        cameraExecutor.shutdown()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_scan, container, false)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    private fun hasReadPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        return ContextCompat.checkSelfPermission(requireContext(), perm) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        previewView = view.findViewById(R.id.previewView)
        btnTake = view.findViewById(R.id.btn_take_photo)
        btnGallery = view.findViewById(R.id.btn_pick_gallery)
        cardClassification = view.findViewById(R.id.card_classification)
        tvClassResult = view.findViewById(R.id.tv_class_result)
        tvClassNote = view.findViewById(R.id.tv_class_note)
        btnSaveHistory = view.findViewById(R.id.btn_save_history)
        ivCropped = view.findViewById(R.id.iv_cropped)
        croppedCard = view.findViewById(R.id.card_cropped_preview)
        btnInfoDropdown = view.findViewById(R.id.btn_info_dropdown)
        ivDropdownArrow = view.findViewById(R.id.iv_dropdown_arrow)
        infoContentContainer = view.findViewById(R.id.info_content_container)
        tvClassInitial = view.findViewById(R.id.tv_class_initial)
        cardInitialCircle = view.findViewById(R.id.card_initial_circle)
        tvClassNote = view.findViewById(R.id.tv_class_note)


        // ---- GALLERY PICKER ----
        pickGalleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                val internal = copyToCache(it)
                showCropOrSkipDialog(internal)
            }
        }

        // ---- UCROP RESULT ----
        uCropLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val output = UCrop.getOutput(result.data!!)
                output?.let { onCropped(it) }
            }
        }

        btnTake.setOnClickListener {
            captureImage()
        }

        btnGallery.setOnClickListener {
            if (!hasReadPermission()) requestReadPermissionThenGallery() else pickGalleryLauncher.launch("image/*")
        }

        btnSaveHistory.setOnClickListener {
            lastCroppedUri?.let { uri ->
                btnSaveHistoryClicked(uri)
            }
        }
        btnInfoDropdown.setOnClickListener {
            if (infoContentContainer.visibility == View.VISIBLE) {
                // Hide dropdown
                infoContentContainer.visibility = View.GONE
                // Rotate arrow back
                ivDropdownArrow.animate().rotation(0f).start()
            } else {
                // Show dropdown
                infoContentContainer.visibility = View.VISIBLE
                // Rotate arrow downwards
                ivDropdownArrow.animate().rotation(180f).start()
            }
        }


        if (!hasCameraPermission()) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            startCamera()
        }

        cardClassification.visibility = View.GONE
        croppedCard.visibility = View.GONE
        btnSaveHistory.isEnabled = false
    }

    private fun requestReadPermissionThenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pickGalleryLauncher.launch("image/*")
            } else {
                Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        launcher.launch(permission)
    }

    private fun resolveSeverity(label: String?, score: Float?): String {
        if (label == null || score == null) return "neutral"

        return when (label.lowercase()) {
            "benign" -> if (score >= 0.85f) "benign" else "neutral"
            "malignant" -> if (score >= 0.85f) "malignant" else "suspicious"
            else -> "neutral"
        }
    }

    // ---------------- CAMERA SETUP --------------------

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(requireContext())
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun captureImage() {
        val cap = imageCapture ?: return

        val file = File(requireContext().cacheDir, "CAP_${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(file).build()

        cap.takePicture(opts, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                val uri = Uri.fromFile(file)
                requireActivity().runOnUiThread {
                    showCropOrSkipDialog(uri)
                }
            }
        })
    }

    // ---------------- IMAGE / CROP --------------------

    private fun copyToCache(uri: Uri): Uri {
        val input = requireContext().contentResolver.openInputStream(uri)
        val dest = File(requireContext().cacheDir, "IMG_${System.currentTimeMillis()}.jpg")
        input.use { inp -> dest.outputStream().use { out -> inp?.copyTo(out) } }
        return Uri.fromFile(dest)
    }

    private fun showCropOrSkipDialog(uri: Uri) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Crop image?")
            .setMessage("Do you want to crop before classification?")
            .setPositiveButton("Crop") { _, _ -> startCrop(uri) }
            .setNegativeButton("Skip") { _, _ -> onCropped(uri) }
            .create()

        dialog.setOnShowListener {
            val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
        }

        dialog.show()
    }


    private fun startCrop(src: Uri) {
        val dest = File(requireContext().cacheDir, "CROP_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(dest)

        val u = UCrop.of(src, destUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1024, 1024)

        uCropLauncher.launch(u.getIntent(requireContext()))
    }

    private fun onCropped(uri: Uri) {
        lastCroppedUri = uri
        croppedCard.visibility = View.VISIBLE
        ivCropped.setImageURI(uri)

        classifyImage(uri)

        btnSaveHistory.isEnabled = true
        // Hide the dropdown button
        view?.findViewById<MaterialCardView>(R.id.btn_info_dropdown)?.visibility = View.GONE
    }

    // ---------------- CLASSIFICATION --------------------

    private fun classifyImage(uri: Uri) {
        val bitmap = requireContext().contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it)
        } ?: return

        val result = tflite.classify(bitmap)
        lastClassificationLabel = result.label
        lastClassificationScore = result.score

        val severity = resolveSeverity(result.label, result.score)

        // Show card
        cardClassification.visibility = View.VISIBLE

        // Update main text (Severity: Benign / Suspicious / Malignant)
        tvClassResult.text = severity.replaceFirstChar { it.uppercase() }

        // Update small note
        tvClassNote.text = "Confidence: ${(result.score * 100).toInt()}%"

        // Update initial circle letter
        tvClassInitial.text = getSeverityInitial(severity)

        // Update circle background color
        val bgColor = ContextCompat.getColor(requireContext(), getSeverityColor(severity))
        cardInitialCircle.setCardBackgroundColor(bgColor)
    }

    private fun getSeverityInitial(severity: String): String {
        return severity.first().uppercase()
    }

    private fun getSeverityColor(severity: String): Int {
        return when (severity) {
            "benign" -> R.color.chip_benign
            "malignant" -> R.color.chip_malignant
            "suspicious" -> R.color.chip_suspicious
            else -> R.color.chip_neutral
        }
    }


    // ---------------- SAVE TO HISTORY --------------------

    private fun saveNewScan(uri: Uri, imgName: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid

        val id = System.currentTimeMillis()
        val folder = File(requireContext().filesDir, "scans/$id/images")
        folder.mkdirs()

        val dest = File(folder, "image_${System.currentTimeMillis()}.jpg")
        requireContext().contentResolver.openInputStream(uri).use { inp ->
            dest.outputStream().use { out -> inp?.copyTo(out) }
        }

        val timestamp = Date()
        val isoString = DateFormat.format("yyyy-MM-dd'T'HH:mm:ss'Z'", timestamp).toString()

        val mainImage = ScanImage(
            path = dest.absolutePath,
            timestamp = isoString, // instead of timestamp.toInstant().toString()
            label = lastClassificationLabel,
            score = lastClassificationScore
        )

        val record = ScanHistory(
            id = id,
            mainImage = mainImage,
            dateIso = isoString,
            imgName = imgName,
            notes = "",
            images = listOf(mainImage)
        )


        // --- Add to ViewModel ---
        sharedViewModel.addScan(record)

        // --- Upload to Firestore ---
        val scanData = hashMapOf(
            "userId" to uid,
            "imagePath" to dest.absolutePath,
            "diagnosis" to (lastClassificationLabel ?: "Unknown"),
            "score" to lastClassificationScore,   // ← ADD THIS
            "imgName" to imgName,
            "createdAt" to timestamp
        )


        db.collection("scans").document(id.toString())
            .set(scanData)
            .addOnSuccessListener {
                Snackbar.make(requireView(), "Saved to new scan and uploaded to Firestore", Snackbar.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(requireView(), "Failed to upload: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    // prompt imgName
    private fun promptForNewScanName(uri: Uri) {
        val input = EditText(requireContext()).apply {
            hint = "Enter image name"
            // optional: kapitalisasi tiap kata
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        }

        AlertDialog.Builder(requireContext())
            .setTitle("New Scan Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val rawName = input.text.toString().trim()
                // fallback kalau user kosongin nama
                val finalName = rawName.ifBlank {
                    // contoh default: pakai tanggal
                    val now = Date()
                    DateFormat.format("yyyy-MM-dd HH:mm", now).toString()
                }
                saveNewScan(uri, finalName)
            }
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    val color = resources.getColor(
                        R.color.medium_sky_blue,
                        requireContext().theme
                    )
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
                    getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
                }
                show()
            }
    }

// ---------------- SAVE TO HISTORY --------------------

    private fun btnSaveHistoryClicked(uri: Uri) {
        val scans = sharedViewModel.history.value ?: emptyList()

        if (scans.isEmpty()) {
            // No existing scans → directly create a new one
            promptForNewScanName(uri)
            return
        }

        // Show dialog with options
        AlertDialog.Builder(requireContext())
            .setTitle("Save Scan")
            .setMessage("Do you want to create a new scan history or add to an existing one?")
            .setPositiveButton("New") { _, _ -> promptForNewScanName(uri) }
            .setNeutralButton("Existing") { _, _ -> chooseExistingScan(uri) }
            .setNegativeButton("Cancel", null)
            .create()
            .apply {
                setOnShowListener {
                    val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
                    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
                    getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(color)
                    getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
                }
                show()
            }
    }

// ---------------- CHOOSE EXISTING SCAN --------------------

    private fun chooseExistingScan(uri: Uri) {
        val scans = sharedViewModel.history.value ?: emptyList()
        if (scans.isEmpty()) {
            promptForNewScanName(uri)
            return
        }

        val listNames = scans.map { s ->
            val imgName = s.imgName
                ?.takeIf { it.isNotBlank() }
                ?: "Untitled scan"

            val label = s.mainImage?.label ?: "No Label"
            "$imgName — $label"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Existing Scan")
            .setItems(listNames) { _, idx ->
                if (idx in scans.indices) {
                    val scan = scans[idx]
                    addImageToExistingScan(uri, scan)
                }
            }
            .show()
    }

    private fun addImageToExistingScan(uri: Uri, scan: ScanHistory) {
        val folder = File(requireContext().filesDir, "scans/${scan.id}/images")
        folder.mkdirs()

        val dest = File(folder, "image_${System.currentTimeMillis()}.jpg")
        requireContext().contentResolver.openInputStream(uri).use { inp ->
            dest.outputStream().use { out -> inp?.copyTo(out) }
        }

        val timestamp = Date()
        val isoString = DateFormat.format("yyyy-MM-dd'T'HH:mm:ss'Z'", timestamp).toString()

        val mainImage1 = ScanImage(
            path = dest.absolutePath,
            timestamp = isoString,
            label = lastClassificationLabel,
            score = lastClassificationScore
        )

        val updatedScan = scan.copy(
            mainImage = mainImage1,
            images = scan.images + mainImage1
            // imgName tetap sama
        )

        val newList = sharedViewModel.history.value?.toMutableList() ?: mutableListOf()
        val idx = newList.indexOfFirst { it.id == scan.id }
        if (idx != -1) {
            newList[idx] = updatedScan
            sharedViewModel.updateScanList(newList)
        }

        // --- Upload new image to Firestore ---
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid

        val safeImgName = scan.imgName
            ?.takeIf { it.isNotBlank() }
            ?: "Untitled scan"

        val scanData = hashMapOf(
            "userId" to uid,
            "imagePath" to dest.absolutePath,
            "diagnosis" to (lastClassificationLabel ?: "Unknown"),
            "score" to lastClassificationScore,
            "imgName" to safeImgName,
            "createdAt" to timestamp
        )

        db.collection("scans").document("${scan.id}_${mainImage1.timestamp.hashCode()}")
            .set(scanData)
            .addOnSuccessListener {
                Snackbar.make(
                    requireView(),
                    "Added image to \"$safeImgName\" and uploaded to Firestore",
                    Snackbar.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { e ->
                Snackbar.make(
                    requireView(),
                    "Failed to upload: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

}

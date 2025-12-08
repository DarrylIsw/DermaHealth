package com.example.dermahealth.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.example.dermahealth.R
import com.example.dermahealth.adapter.HistoryAdapter
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.databinding.FragmentHistoryBinding
import com.example.dermahealth.helper.BackHandler
import com.example.dermahealth.ui.SwipeActionsCallback
import com.example.dermahealth.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class HistoryFragment : Fragment(), BackHandler {

    override fun onBackPressed(): Boolean = false

    private var _b: FragmentHistoryBinding? = null
    private val b get() = _b!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val adapter by lazy {
        HistoryAdapter(
            onEdit = { scan -> showEditDialog(scan) },
            onDelete = { scan -> confirmDelete(scan) },
            onArchive = { scan -> confirmArchive(scan) },
            onToggleExpand = { pos, expanded ->
                if (expanded) {
                    b.rvHistory.post { b.rvHistory.smoothScrollToPosition(pos) }
                }
            }
        )
    }

    private fun showEditDialog(scan: ScanHistory) {
        val context = requireContext()

        // Buat EditText untuk edit catatan
        val input = android.widget.EditText(context).apply {
            setText(scan.notes)
            setSelection(text.length)
            hint = "Enter Notes"
            minLines = 3
            maxLines = 6
            setPadding(40, 30, 40, 30)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(getString(R.string.edit_notes))
            .setMessage("Edit notes for: ${scan.mainImage?.label ?: "Unknown"}")
            .setView(input)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                val newNotes = input.text.toString()

                // Update list di ViewModel
                val currentList = sharedViewModel.history.value?.toMutableList() ?: mutableListOf()
                val idx = currentList.indexOfFirst { it.id == scan.id }
                if (idx != -1) {
                    val updatedScan = scan.copy(notes = newNotes)
                    currentList[idx] = updatedScan
                    sharedViewModel.updateScanList(currentList)
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }


    private fun confirmDelete(scan: ScanHistory) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_scan_title)
            .setMessage(R.string.delete_scan_msg)
            .setPositiveButton(R.string.yes) { _, _ -> removeWithUndo(scan) }
            .setNegativeButton(R.string.no, null)
            .create()

        dialog.setOnShowListener {
            val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
        }

        dialog.show()
    }

    private fun confirmArchive(scan: ScanHistory) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.archive_scan_title)
            .setMessage(getString(R.string.archive_scan_msg))
            .setPositiveButton(R.string.archive_scan) { _, _ -> archiveScan(scan) }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val color = resources.getColor(R.color.medium_sky_blue, requireContext().theme)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(color)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(color)
        }

        dialog.show()
    }

    private fun archiveScan(scan: ScanHistory) {
        // 1️⃣ Remove from adapter list
        val currentList = adapter.currentList.toMutableList()
        val idx = currentList.indexOfFirst { it.id == scan.id }
        if (idx == -1) return

        currentList.removeAt(idx)
        adapter.submitList(currentList)
        updateEmptyState()

        // 2️⃣ Pindahkan file-image ke folder archive lokal
        try {
            val archiveRoot = File(requireContext().filesDir, "archived_scans")
            if (!archiveRoot.exists()) archiveRoot.mkdirs()

            val archiveFolder = File(archiveRoot, scan.id.toString())
            if (!archiveFolder.exists()) archiveFolder.mkdirs()

            scan.images.forEach { img ->
                val src = File(img.path)
                if (src.exists()) {
                    val dest = File(archiveFolder, src.name)
                    try {
                        // copy lalu hapus asal; kalau mau lebih aman, bisa hanya copy
                        src.copyTo(dest, overwrite = true)
                        src.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3️⃣ Remove dari "active history" di persistent storage
        // Untuk sekarang, kita perlakukan archive seperti "soft delete" dari history utama
        sharedViewModel.deleteScan(scan)

        // 4️⃣ Feedback ke user
        Snackbar.make(b.root, getString(R.string.scan_archived), Snackbar.LENGTH_LONG).show()
    }


    private fun removeWithUndo(scan: ScanHistory) {
        val currentList = adapter.currentList.toMutableList()
        val idx = currentList.indexOfFirst { it.id == scan.id }
        if (idx == -1) return

        // 1️⃣ Remove from adapter immediately
        currentList.removeAt(idx)
        adapter.submitList(currentList)
        updateEmptyState()

        // 2️⃣ Delete files from disk immediately
        scan.images.forEach { img ->
            try { File(img.path).delete() } catch (e: Exception) { e.printStackTrace() }
        }

        // 3️⃣ Remove from persistent storage
        sharedViewModel.deleteScan(scan)

        // 4️⃣ Show Snackbar with undo
        Snackbar.make(b.root, getString(R.string.scan_deleted), Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                // Restore in adapter
                val restored = adapter.currentList.toMutableList()
                restored.add(idx.coerceIn(0, restored.size), scan)
                adapter.submitList(restored)
                updateEmptyState()

                // Restore in persistent storage
                sharedViewModel.addScan(scan)

                // Optionally restore files if you want (optional)
                // copy files back from backup location if implemented
            }
            .show()
    }

    private fun updateStatisticsInFirebase(scans: List<ScanHistory>) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        val db = FirebaseFirestore.getInstance()

        val totalScans = scans.size
        var benign = 0
        var neutral = 0
        var suspicious = 0
        var malignant = 0

        // For overall skin score
        var totalScore = 0f

        scans.forEach { scan ->
            val label = scan.mainImage?.label?.lowercase() ?: "neutral"

            when (label) {
                "benign" -> benign++
                "neutral" -> neutral++
                "suspicious" -> suspicious++
                "malignant" -> malignant++
            }

            totalScore += scan.mainImage?.score ?: 0f
        }

        val overallSkinScore = if (totalScans > 0) {
            ((totalScore / totalScans).coerceIn(0f, 1f) * 100f)
        } else 0f

        val statsData = hashMapOf(
            "userId" to uid,
            "totalScans" to totalScans,
            "benignCount" to benign,
            "neutralCount" to neutral,
            "suspiciousCount" to suspicious,
            "malignantCount" to malignant,
            "overallSkinScore" to overallSkinScore.toInt(),
            "lastUpdated" to FieldValue.serverTimestamp()
        )

        db.collection("statistics").document(uid)
            .set(statsData)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentHistoryBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // RecyclerView setup
        b.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        b.rvHistory.adapter = adapter
        b.rvHistory.setHasFixedSize(false)
        (b.rvHistory.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        // Extra bottom padding
        val extra = resources.getDimensionPixelSize(R.dimen.rv_extra_scroll)
        b.rvHistory.post {
            b.rvHistory.updatePadding(bottom = b.rvHistory.paddingBottom + extra)
        }

        // Observe ViewModel — replaces seed list
        sharedViewModel.history.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            updateEmptyState()
            updateStatisticsInFirebase(list)
        }
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        b.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}

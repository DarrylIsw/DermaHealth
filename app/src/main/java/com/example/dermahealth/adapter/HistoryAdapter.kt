package com.example.dermahealth.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.dermahealth.R
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.data.ScanImage
import com.example.dermahealth.databinding.ItemHistoryBinding
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoryAdapter(
    private val onEdit: (ScanHistory) -> Unit,
    private val onDelete: (ScanHistory) -> Unit,
    private val onArchive: (ScanHistory) -> Unit,
    private val onUnarchive: (ScanHistory) -> Unit,
    private val onToggleExpand: (position: Int, expanded: Boolean) -> Unit
) : ListAdapter<ScanHistory, HistoryAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ScanHistory>() {
            override fun areItemsTheSame(old: ScanHistory, new: ScanHistory) = old.id == new.id
            override fun areContentsTheSame(old: ScanHistory, new: ScanHistory) = old == new
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private val IN_FMT = DateTimeFormatter.ISO_LOCAL_DATE
        @RequiresApi(Build.VERSION_CODES.O)
        private val OUT_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy")

        @RequiresApi(Build.VERSION_CODES.O)
        fun formatIsoDate(iso: String?): String {
            if (iso.isNullOrBlank()) return "Unknown"
            return try {
                // Parse ISO 8601 datetime like "2025-12-05T22:07:18Z"
                val sdfInput = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                sdfInput.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val date = sdfInput.parse(iso)

                // Format to readable string
                val sdfOutput = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                sdfOutput.format(date!!)
            } catch (e: Exception) {
                iso
            }
        }
    }

    inner class VH(val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root)

    // -------------------------------------------------
    // 🎨 Conclusion UI styling
    // -------------------------------------------------
    data class ConclusionStyle(
        val icon: Int,
        val background: Int,
        val text: String
    )

    private fun resolveSeverity(label: String?, score: Float?): String {
        if (label.isNullOrBlank()) return "neutral"
        return when (label.lowercase()) {
            "benign" -> if (score != null && score >= 0.85f) "benign" else "neutral"
            "malignant" -> if (score != null && score >= 0.85f) "malignant" else "suspicious"
            "suspicious" -> "suspicious"
            else -> "neutral"
        }
    }


    private fun getConclusionUI(label: String?, score: Float?): ConclusionStyle {
        val s = resolveSeverity(label, score)

        return when (s) {
            "benign" -> ConclusionStyle(
                icon = R.drawable.ic_check_circle,
                background = R.drawable.bg_conclusion_safe,
                text = "Benign (Low Risk)"
            )

            "malignant" -> ConclusionStyle(
                icon = R.drawable.ic_error,
                background = R.drawable.bg_conclusion_danger,
                text = "Malignant — Seek Medical Attention"
            )

            "suspicious" -> ConclusionStyle(
                icon = R.drawable.ic_warning,
                background = R.drawable.bg_conclusion_warning,
                text = "Suspicious — Monitor Carefully"
            )

            else -> ConclusionStyle(
                icon = R.drawable.ic_info,
                background = R.drawable.bg_conclusion_neutral,
                text = "Neutral / Low Confidence"
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(
            ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b
        b.tvScanNumber.text = "Scan #${position + 1}"
        b.tvAnalysis.text = item.notes.ifEmpty { "No notes added" }
        b.tvNotes.text = if (item.notes.isNullOrBlank()) {
            "No Notes"
        } else {
            "View Notes"
        }

        val main = item.mainImage

        // MAIN IMAGE
        if (main != null) {
            b.ivScan.load(File(main.path)) {
                crossfade(true)
                transformations(RoundedCornersTransformation(14f))
            }
        }

        val label = main?.label ?: "Unknown"
        val score = main?.score

// CHIP COLOR + TEXT
        val severity = resolveSeverity(label, score)
        val chipColor = when (severity) {
            "benign" -> R.color.chip_benign
            "malignant" -> R.color.chip_malignant
            "suspicious" -> R.color.chip_suspicious
            else -> R.color.chip_neutral
        }

// Use severity as display text instead of raw label
        val chipText = when (severity) {
            "benign" -> "Benign"
            "malignant" -> "Malignant"
            "suspicious" -> "Suspicious"
            else -> "Neutral"
        }

        b.chipResult.text = chipText
        b.chipResult.setChipBackgroundColorResource(chipColor)

        // ---------- IMAGE NAME (imgName) ----------
        val imgName = item.imgName
            ?.takeIf { it.isNotBlank() }
            ?: "Untitled scan"

        b.tvImgName.text = imgName

        // DATE + NOTES
        b.tvDate.text = formatIsoDate(item.dateIso)

        // ---------- SCORE ----------
        val avgScore = if (item.images.isNotEmpty()) {
            item.images.mapNotNull { it.score }.average()
        } else 0.0
        val avgPct = (avgScore * 100).toInt()
        b.tvAverageScore.text = "Score: $avgPct%"

        // ---------- CONCLUSION UI ----------
        val ui = getConclusionUI(label, score)
        b.conclusionBox.setBackgroundResource(ui.background)
        b.ivStatusIcon.setImageResource(ui.icon)
        b.tvConclusion.text = ui.text

        // ---------- IMAGE LIST WITH INDIVIDUAL SCORES ----------
        b.rvImages.layoutManager =
            LinearLayoutManager(b.root.context, LinearLayoutManager.HORIZONTAL, false)
        // NEWEST -> OLDEST (kiri ke kanan)
        val imagesNewestFirst = item.images.asReversed()
        b.rvImages.adapter = HistoryImagesAdapter(imagesNewestFirst)
        b.rvImages.scrollToPosition(0)

        b.rvImages.isNestedScrollingEnabled = false

        // ---------- EXPAND / COLLAPSE ----------
        b.expandable.isVisible = item.isExpanded
        b.btnExpand.rotation = if (item.isExpanded) 180f else 0f

        b.btnExpand.setOnClickListener {
            val idx = holder.bindingAdapterPosition
            if (idx == RecyclerView.NO_POSITION) return@setOnClickListener

            val updated = item.copy(isExpanded = !item.isExpanded)
            val newList = currentList.toMutableList()
            newList[idx] = updated
            submitList(newList) { onToggleExpand(idx, updated.isExpanded) }
        }

        b.btnEdit.setOnClickListener { onEdit(item) }
        b.btnDelete.setOnClickListener { onDelete(item) }

        // ---------- ARCHIVE / UNARCHIVE ----------
        if (item.isArchived) {
            // Mode archived: tampilkan tombol UNARCHIVE
            b.btnArchive.visibility = View.VISIBLE
            b.btnArchive.text = b.root.context.getString(R.string.unarchive_history)
            b.btnArchive.setTextColor(
                b.root.context.getColor(R.color.medium_sky_blue)
            )
            b.btnArchive.setOnClickListener { onUnarchive(item) }
        } else {
            // Mode normal: tombol ARCHIVE
            b.btnArchive.visibility = View.VISIBLE
            b.btnArchive.text = b.root.context.getString(R.string.archive_history)
            b.btnArchive.setTextColor(
                b.root.context.getColor(R.color.medium_sky_blue)
            )
            b.btnArchive.setOnClickListener { onArchive(item) }
        }
    }
}

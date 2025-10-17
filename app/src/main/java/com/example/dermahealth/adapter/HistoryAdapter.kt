package com.example.dermahealth.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.dermahealth.R
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.databinding.ItemHistoryBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HistoryAdapter(
    private val onEdit: (ScanHistory) -> Unit,             // Callback when edit button is clicked
    private val onDelete: (ScanHistory) -> Unit,           // Callback when delete button is clicked
    private val onToggleExpand: (position: Int, expanded: Boolean) -> Unit // Callback for expand/collapse
) : ListAdapter<ScanHistory, HistoryAdapter.VH>(DIFF) {

    companion object {
        // DiffUtil to optimize RecyclerView updates
        private val DIFF = object : DiffUtil.ItemCallback<ScanHistory>() {
            override fun areItemsTheSame(old: ScanHistory, new: ScanHistory) = old.id == new.id
            override fun areContentsTheSame(old: ScanHistory, new: ScanHistory) = old == new
        }

        // Date formatting helpers (requires Android O+)
        @RequiresApi(Build.VERSION_CODES.O)
        private val IN_FMT = DateTimeFormatter.ISO_LOCAL_DATE  // Input format from backend/DB

        @RequiresApi(Build.VERSION_CODES.O)
        private val OUT_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy") // Display format

        @RequiresApi(Build.VERSION_CODES.O)
        fun prettyDate(iso: String?): String = runCatching {
            if (iso.isNullOrBlank()) return@runCatching "Unknown"
            LocalDate.parse(iso, IN_FMT).format(OUT_FMT)
        }.getOrElse { iso ?: "Unknown" } // Safe fallback
    }

    // ViewHolder using ViewBinding for cleaner access to views
    inner class VH(val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemHistoryBinding.inflate(inflater, parent, false) // Inflate layout
        return VH(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b
        val context = b.root.context

        // --- 🖼️ Load main scan image with Coil
        if (!item.imageUrl.isNullOrBlank()) {
            b.ivScan.load(item.imageUrl) {
                crossfade(true)
                transformations(RoundedCornersTransformation(12f)) // Rounded corners
                error(R.drawable.dummy_melanoma)                  // Fallback image
                placeholder(R.drawable.dummy_melanoma)            // Placeholder while loading
            }
        }

        // --- 🖼️ Load cropped image or placeholder
        b.ivCropped.load(item.croppedUrl ?: R.drawable.bg_image_placeholder) {
            crossfade(true)
            transformations(RoundedCornersTransformation(12f))
            error(R.drawable.bg_image_placeholder)
            placeholder(R.drawable.bg_image_placeholder)
        }

        // --- 📝 Set notes and formatted date
        b.tvNotes.text = item.notes.ifBlank { "No notes provided." }
        b.tvDate.text = prettyDate(item.dateIso)

        // --- 🩺 Set result chip text and background color
        b.chipResult.text = item.result
        val colorRes = when (item.result.lowercase()) {
            "benign" -> R.color.chip_benign
            "suspicious" -> R.color.chip_suspicious
            "malignant" -> R.color.chip_malignant
            else -> R.color.chip_neutral
        }
        b.chipResult.setChipBackgroundColorResource(colorRes)

        // --- 🧠 Dynamic conclusion section based on result
        when (item.result.lowercase()) {
            "benign" -> {
                b.conclusionBox.setBackgroundResource(R.drawable.bg_conclusion_safe)
                b.tvConclusion.text = "Safe — No action needed"
                b.ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
            }
            "suspicious" -> {
                b.conclusionBox.setBackgroundResource(R.drawable.bg_conclusion_warning)
                b.tvConclusion.text = "Potential issue — Monitor closely"
                b.ivStatusIcon.setImageResource(R.drawable.ic_warning)
            }
            "malignant" -> {
                b.conclusionBox.setBackgroundResource(R.drawable.bg_conclusion_danger)
                b.tvConclusion.text = "Danger — Seek medical attention"
                b.ivStatusIcon.setImageResource(R.drawable.ic_error)
            }
            "neutral" -> {
                b.conclusionBox.setBackgroundResource(R.drawable.bg_conclusion_neutral)
                b.tvConclusion.text = "Normal — No abnormality detected"
                b.ivStatusIcon.setImageResource(R.drawable.ic_info)
            }
            else -> {
                // Default fallback
                b.conclusionBox.setBackgroundResource(R.drawable.bg_conclusion_neutral)
                b.tvConclusion.text = "No abnormality detected"
                b.ivStatusIcon.setImageResource(R.drawable.ic_info)
            }
        }

        // --- 🔽 Expand / Collapse section
        b.expandable.isVisible = item.isExpanded
        b.btnExpand.animate().rotation(if (item.isExpanded) 180f else 0f).setDuration(150).start()

        // Toggle expansion state
        b.btnExpand.setOnClickListener {
            val idx = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener

            // Update list with new expanded state
            val newList = currentList.toMutableList()
            val current = newList[idx]
            val updated = current.copy(isExpanded = !current.isExpanded)
            newList[idx] = updated

            submitList(newList) { onToggleExpand(idx, updated.isExpanded) }
        }

        // --- ✏️ Button actions
        b.btnEdit.setOnClickListener { onEdit(item) }
        b.btnDelete.setOnClickListener { onDelete(item) }
    }
}

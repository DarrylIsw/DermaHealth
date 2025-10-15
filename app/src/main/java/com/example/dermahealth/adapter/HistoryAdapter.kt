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
    private val onEdit: (ScanHistory) -> Unit,
    private val onDelete: (ScanHistory) -> Unit,
    private val onToggleExpand: (position: Int, expanded: Boolean) -> Unit
) : ListAdapter<ScanHistory, HistoryAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ScanHistory>() {
            override fun areItemsTheSame(old: ScanHistory, new: ScanHistory) = old.id == new.id
            override fun areContentsTheSame(old: ScanHistory, new: ScanHistory) = old == new
        }

        // Date formatters — safe for Android O and above
        @RequiresApi(Build.VERSION_CODES.O)
        private val IN_FMT = DateTimeFormatter.ISO_LOCAL_DATE

        @RequiresApi(Build.VERSION_CODES.O)
        private val OUT_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy")

        @RequiresApi(Build.VERSION_CODES.O)
        fun prettyDate(iso: String?): String = runCatching {
            if (iso.isNullOrBlank()) return@runCatching "Unknown"
            LocalDate.parse(iso, IN_FMT).format(OUT_FMT)
        }.getOrElse { iso ?: "Unknown" }
    }

    inner class VH(val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemHistoryBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b
        val context = b.root.context

        // --- 🖼️ Load Images (safe fallback and rounded corners)
        if (!item.imageUrl.isNullOrBlank()) {
            b.ivScan.load(item.imageUrl) {
                crossfade(true)
                transformations(RoundedCornersTransformation(12f))
                error(R.drawable.dummy_melanoma)
                placeholder(R.drawable.dummy_melanoma)
            }
        }

        b.ivCropped.load(item.croppedUrl ?: R.drawable.bg_image_placeholder) {
            crossfade(true)
            transformations(RoundedCornersTransformation(12f))
            error(R.drawable.bg_image_placeholder)
            placeholder(R.drawable.bg_image_placeholder)
        }

        // --- 📝 Texts
        b.tvNotes.text = item.notes.ifBlank { "No notes provided." }
        b.tvDate.text = prettyDate(item.dateIso)

        // --- 🩺 Result Chip
        b.chipResult.text = item.result
        val colorRes = when (item.result.lowercase()) {
            "benign" -> R.color.chip_benign
            "suspicious" -> R.color.chip_suspicious
            "malignant" -> R.color.chip_malignant
            else -> R.color.chip_neutral
        }
        b.chipResult.setChipBackgroundColorResource(colorRes)

        // --- 🧠 Dynamic Conclusion Section
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

        // --- 🔽 Expand / Collapse
        b.expandable.isVisible = item.isExpanded
        b.btnExpand.animate().rotation(if (item.isExpanded) 180f else 0f).setDuration(150).start()

        b.btnExpand.setOnClickListener {
            val idx = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener

            val newList = currentList.toMutableList()
            val current = newList[idx]
            val updated = current.copy(isExpanded = !current.isExpanded)
            newList[idx] = updated

            submitList(newList) {
                onToggleExpand(idx, updated.isExpanded)
            }
        }

        // --- ✏️ Actions
        b.btnEdit.setOnClickListener { onEdit(item) }
        b.btnDelete.setOnClickListener { onDelete(item) }
    }

}

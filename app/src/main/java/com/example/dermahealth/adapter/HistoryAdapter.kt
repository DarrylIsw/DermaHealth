package com.example.dermahealth.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isGone
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

        @RequiresApi(Build.VERSION_CODES.O)
        private val IN_FMT = DateTimeFormatter.ISO_LOCAL_DATE
        @RequiresApi(Build.VERSION_CODES.O)
        private val OUT_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy")
        @RequiresApi(Build.VERSION_CODES.O)
        fun prettyDate(iso: String): String = runCatching {
            LocalDate.parse(iso, IN_FMT).format(OUT_FMT)
        }.getOrElse { iso }
    }

    inner class VH(val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.b

        // images
        b.ivScan.load(item.imageUrl ?: R.drawable.ic_launcher_foreground) {
            crossfade(true)
            transformations(RoundedCornersTransformation(12f))
            error(R.drawable.bg_image_placeholder)
            placeholder(R.drawable.bg_image_placeholder)
        }
        b.ivCropped.load(item.croppedUrl ?: R.drawable.ic_launcher_background) {
            crossfade(true)
            error(R.drawable.bg_image_placeholder)
            placeholder(R.drawable.bg_image_placeholder)
        }

        // text
        b.tvNotes.text = item.notes
        b.tvDate.text = prettyDate(item.dateIso)

        // result chip styling
        b.chipResult.text = item.result
        when (item.result.lowercase()) {
            "benign" -> b.chipResult.setChipBackgroundColorResource(R.color.chip_benign)
            "suspicious" -> b.chipResult.setChipBackgroundColorResource(R.color.chip_suspicious)
            "malignant" -> b.chipResult.setChipBackgroundColorResource(R.color.chip_malignant)
            else -> b.chipResult.setChipBackgroundColorResource(R.color.chip_neutral)
        }

        // expand state
        b.expandable.isVisible = item.isExpanded
        b.btnExpand.rotation = if (item.isExpanded) 180f else 0f

        // expand/collapse with smooth layout change
        b.btnExpand.setOnClickListener {
            val idx = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
            val newList = currentList.toMutableList()
            val cur = newList[idx]
            val next = cur.copy(isExpanded = !cur.isExpanded)
            newList[idx] = next

            submitList(newList) {
                // notify fragment AFTER RV has applied the diff & layout pass
                onToggleExpand(idx, next.isExpanded)
            }
        }

        // actions
        b.btnEdit.setOnClickListener { onEdit(item) }
        b.btnDelete.setOnClickListener { onDelete(item) }
    }
}

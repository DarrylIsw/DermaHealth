package com.example.dermahealth.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R
import com.example.dermahealth.data.ScanHistory

class HistoryAdapter(
    private val scans: MutableList<ScanHistory>,
    private val onEdit: (ScanHistory) -> Unit,
    private val onDelete: (ScanHistory) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivScan: ImageView = itemView.findViewById(R.id.iv_scan)
        val tvResult: TextView = itemView.findViewById(R.id.tv_result)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val tvNotes: TextView = itemView.findViewById(R.id.tv_notes)
        val btnExpand: ImageView = itemView.findViewById(R.id.btn_expand)
        val expandable: LinearLayout = itemView.findViewById(R.id.expandable_section)
        val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        val btnDelete: Button = itemView.findViewById(R.id.btn_delete)

        // Extra detail views
        val tvConfidence: TextView = itemView.findViewById(R.id.tvConfidence)
        val tvAnalysis: TextView = itemView.findViewById(R.id.tvAnalysis)
        val tvConclusion: TextView = itemView.findViewById(R.id.tvConclusion)
        val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        val conclusionBox: LinearLayout = itemView.findViewById(R.id.conclusionBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val scan = scans[position]

        // --- Basic info ---
        holder.ivScan.setImageResource(scan.imageRes)
        holder.tvResult.text = "Result: ${scan.result}"
        holder.tvDate.text = "Date: ${scan.date}"
        holder.tvNotes.text = "Notes: ${scan.notes}"

        // --- Dynamic details based on scan result ---
        if (scan.result == "Benign") {
            holder.tvConfidence.text = "Detection Confidence: 92%"
            holder.tvAnalysis.text =
                "The lesion appears smooth and consistent in color, indicating a benign pattern."
            holder.tvConclusion.text = "Safe — No action needed"
            holder.conclusionBox.setBackgroundResource(R.drawable.bg_conclusion_safe)
            holder.ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
        } else {
            holder.tvConfidence.text = "Detection Confidence: 68%"
            holder.tvAnalysis.text =
                "Irregular edges and uneven coloration suggest a suspicious lesion pattern."
            holder.tvConclusion.text = "Warning — Visit dermatologist soon"
            holder.conclusionBox.setBackgroundResource(R.drawable.bg_conclusion_danger)
            holder.ivStatusIcon.setImageResource(R.drawable.ic_warning)
        }

        // --- Expandable section ---
        var expanded = false
        holder.btnExpand.setOnClickListener {
            expanded = !expanded
            holder.expandable.visibility = if (expanded) View.VISIBLE else View.GONE
            holder.btnExpand.animate().rotation(if (expanded) 180f else 0f).setDuration(200).start()
        }

        // --- Buttons ---
        holder.btnEdit.setOnClickListener { onEdit(scan) }
        holder.btnDelete.setOnClickListener { onDelete(scan) }
    }

    override fun getItemCount(): Int = scans.size
}

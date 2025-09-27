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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = scans[position]
        holder.ivScan.setImageResource(item.imageRes)
        holder.tvResult.text = "Result: ${item.result}"
        holder.tvDate.text = "Date: ${item.date}"
        holder.tvNotes.text = "Notes: ${item.notes}"

        var expanded = false
        holder.btnExpand.setOnClickListener {
            expanded = !expanded
            holder.expandable.visibility = if (expanded) View.VISIBLE else View.GONE
            holder.btnExpand.setImageResource(
                if (expanded) R.drawable.   ic_expand_less else R.drawable.ic_expand_more
            )
        }

        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = scans.size
}

package com.example.dermahealth.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R
import com.example.dermahealth.data.Routine
import com.example.dermahealth.data.RoutineType
import java.text.SimpleDateFormat
import java.util.*

class RoutineAdapter(
    private val items: MutableList<Routine>,
    private val onEdit: (Routine) -> Unit,
    private val onDelete: (Routine) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_routine_title)
        val tvTime: TextView = view.findViewById(R.id.tv_routine_time)
        val tvComment: TextView = view.findViewById(R.id.et_additional_comment)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_routine, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvTitle.text = item.title

        holder.tvTime.text = when (item.type) {
            RoutineType.HOURLY -> "Every hour"

            RoutineType.EVERY_X_HOURS ->
                "Every ${item.intervalHours ?: 1} hours"

            RoutineType.DAILY -> {
                val h = item.hour ?: 0
                val m = item.minute ?: 0
                String.format("%02d:%02d", h, m)
            }

            RoutineType.EVERY_X_DAYS -> {
                val d = item.intervalDays ?: 1
                val h = item.hour ?: 0
                val m = item.minute ?: 0
                "Every $d day(s) at %02d:%02d".format(h, m)
            }

            RoutineType.SPECIFIC_DATE -> {
                item.specificDate?.let {
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    sdf.format(Date(it))
                } ?: "No date set"
            }

            RoutineType.HOURLY_SPECIFIC_TIME -> {
                val h = item.hour ?: 0
                val m = item.minute ?: 0
                "Every hour at %02d:%02d".format(h, m)
            }

            RoutineType.SPECIFIC_TIME_ONLY -> {
                val h = item.hour ?: 0
                val m = item.minute ?: 0
                "At %02d:%02d".format(h, m)
            }
        }

        if (item.note.isNullOrBlank()) {
            holder.tvComment.visibility = View.GONE
        } else {
            holder.tvComment.visibility = View.VISIBLE
            holder.tvComment.text = item.note
        }

        holder.btnEdit.setOnClickListener { onEdit(item) }

        // Optional: attach a long-press delete (you already have swipe)
        holder.itemView.setOnLongClickListener {
            onDelete(item)
            true
        }

        // Simple entrance animation (keeps your previous behaviour)
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(position * 80L)
            .start()
    }

    override fun getItemCount(): Int = items.size

    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateData(newList: List<Routine>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}

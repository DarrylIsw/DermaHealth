package com.example.dermahealth.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R
import com.example.dermahealth.data.Routine

class RoutineAdapter(
    private val items: MutableList<Routine>,
    private val onEdit: (Routine) -> Unit,
    private val onDelete: (Routine) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_routine_title)
        val tvTime: TextView = view.findViewById(R.id.tv_routine_time)
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
        holder.tvTime.text = item.time
        holder.btnEdit.setOnClickListener { onEdit(item) }

        // --- Fade-in + slide-up animation ---
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

    // For swipe-to-delete or manual removal later
    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // For updating data dynamically
    fun updateData(newList: List<Routine>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }
}

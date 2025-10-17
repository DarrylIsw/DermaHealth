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
    private val items: MutableList<Routine>,          // Mutable list to allow dynamic updates
    private val onEdit: (Routine) -> Unit,            // Callback when edit button is clicked
    private val onDelete: (Routine) -> Unit           // Optional callback for delete (can be used with swipe)
) : RecyclerView.Adapter<RoutineAdapter.VH>() {

    // ViewHolder holds references to item views
    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_routine_title) // Routine title
        val tvTime: TextView = view.findViewById(R.id.tv_routine_time)   // Routine time
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit)      // Edit button
    }

    // Called when RecyclerView needs a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_routine, parent, false) // Inflate item layout
        return VH(v)
    }

    // Bind data to each item
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        // Set title and time
        holder.tvTitle.text = item.title
        holder.tvTime.text = item.time

        // Set edit button click listener
        holder.btnEdit.setOnClickListener { onEdit(item) }

        // --- Fade-in + slide-up animation ---
        // Gives a nice entrance effect for each item
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f
        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)                 // Duration of animation
            .setStartDelay(position * 80L)    // Stagger animation for each item
            .start()
    }

    // Return total number of items
    override fun getItemCount(): Int = items.size

    // --- Helper functions ---

    // Remove item at a specific position (e.g., for swipe-to-delete)
    fun removeAt(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)       // Notify adapter to update RecyclerView
        }
    }

    // Update the whole data set dynamically
    fun updateData(newList: List<Routine>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()               // Refresh entire RecyclerView
    }
}

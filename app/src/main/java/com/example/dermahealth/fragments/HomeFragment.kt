package com.example.dermahealth

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator

class HomeFragment : Fragment() {

    private lateinit var cpSkin: CircularProgressIndicator
    private lateinit var tvScore: TextView
    private lateinit var rvRoutines: RecyclerView
    private lateinit var btnAddRoutine: ImageButton
    private val routineList = mutableListOf<Routine>()
    private lateinit var adapter: RoutineAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cpSkin = view.findViewById(R.id.cp_skin)
        tvScore = view.findViewById(R.id.tv_skin_score)
        rvRoutines = view.findViewById(R.id.rv_routines)
        btnAddRoutine = view.findViewById(R.id.btn_add_routine)

        // Dummy routines
        routineList.add(Routine(1, "Apply sunscreen", "08:00 AM"))
        routineList.add(Routine(2, "Moisturize before bed", "10:00 PM"))
        routineList.add(Routine(3, "Drink more water", "Throughout the day"))

        // RecyclerView setup
        adapter = RoutineAdapter(
            routineList,
            onEdit = { routine ->
                Toast.makeText(requireContext(), "Edit: ${routine.title}", Toast.LENGTH_SHORT).show()
                // TODO: Implement edit dialog/form later
            },
            onDelete = { routine ->
                showDeleteConfirm(routine, adapter) // ✅ now works
            }
        )
        rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        rvRoutines.adapter = adapter

        // Add routine button (dummy)
        btnAddRoutine.setOnClickListener {
            val id = (routineList.maxOfOrNull { it.id } ?: 0) + 1
            val newItem = Routine(id, "New routine $id", "Time")
            routineList.add(0, newItem)
            adapter.notifyItemInserted(0)
            rvRoutines.scrollToPosition(0)
            Toast.makeText(requireContext(), "Routine added (dummy)", Toast.LENGTH_SHORT).show()
        }

        // Quick action: navigate to Scan
        view.findViewById<View>(R.id.btn_quick_scan).setOnClickListener {
            activity?.supportFragmentManager
                ?.beginTransaction()
                ?.replace(R.id.fragment_container, ScanFragment())
                ?.addToBackStack(null)
                ?.commit()
        }

        // Animate skin score (dummy value)
        animateSkinScore(85)
    }
    private fun showDeleteConfirm(routine: Routine, adapter: RoutineAdapter) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.delete_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val index = routineList.indexOfFirst { it.id == routine.id }
                if (index >= 0) {
                    routineList.removeAt(index)
                    adapter.notifyItemRemoved(index)
                    Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun animateSkinScore(target: Int) {
        val animator = ValueAnimator.ofInt(0, target)
        animator.duration = 800
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            tvScore.text = value.toString()
            // try both ways to set progress (compat depends on Material lib version)
            try {
                cpSkin.setProgressCompat(value, true)
            } catch (t: Throwable) {
                // fallback to direct property if available
                try {
                    cpSkin.progress = value
                } catch (_: Throwable) { /* ignore */ }
            }
        }
        animator.start()
    }
}

/** Simple data class and adapter below **/
data class Routine(val id: Int, val title: String, val time: String)

class RoutineAdapter(
    private val items: List<Routine>,
    private val onEdit: (Routine) -> Unit,
    private val onDelete: (Routine) -> Unit
) : RecyclerView.Adapter<RoutineAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_routine_title)
        val tvTime: TextView = view.findViewById(R.id.tv_routine_time)
        val btnEdit: ImageButton = view.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_routine, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvTime.text = item.time
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}

package com.example.dermahealth

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.AlphaAnimation
import android.widget.ImageButton
import android.widget.ImageView
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
    private lateinit var tvTip: TextView
    private lateinit var ivTipIcon: ImageView

    private val routineList = mutableListOf<Routine>()
    private lateinit var adapter: RoutineAdapter

    // --- Tip of the Day rotation ---
    private val tips = listOf(
        "90% of skin aging is caused by sun exposure — wear sunscreen daily.",
        "Drink at least 8 glasses of water a day to keep your skin hydrated.",
        "A balanced diet rich in antioxidants helps your skin glow naturally.",
        "Cleanse your face gently, avoid over-scrubbing.",
        "Sleep is your skin’s best friend — aim for 7–8 hours."
    )
    private var tipIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private val tipInterval = 5000L // 5 seconds

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

        // Tip of the Day views
        tvTip = view.findViewById(R.id.tv_tip)
        ivTipIcon = view.findViewById(R.id.iv_tip_icon)

        // Initial tip
        tvTip.text = tips[tipIndex]

        // Start rotating tips
        startTipRotation()

        // Dummy routines
        routineList.add(Routine(1, "Apply sunscreen", "08:00 AM"))
        routineList.add(Routine(2, "Moisturize before bed", "10:00 PM"))
        routineList.add(Routine(3, "Drink more water", "Throughout the day"))

        // RecyclerView setup
        adapter = RoutineAdapter(
            routineList,
            onEdit = { routine ->
                Toast.makeText(requireContext(), "Edit: ${routine.title}", Toast.LENGTH_SHORT).show()
                // TODO: open edit dialog later
            },
            onDelete = { routine ->
                showDeleteConfirm(routine, adapter)
            }
        )
        rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        rvRoutines.adapter = adapter

        // Add routine button
        btnAddRoutine.setOnClickListener {
            val id = (routineList.maxOfOrNull { it.id } ?: 0) + 1
            val newItem = Routine(id, "New routine $id", "Time")
            routineList.add(0, newItem)
            adapter.notifyItemInserted(0)
            rvRoutines.scrollToPosition(0)
            Toast.makeText(requireContext(), "Routine added (dummy)", Toast.LENGTH_SHORT).show()
        }
        // Animate skin score
        animateSkinScore(85)
    }

    // --- Tip rotation with fade animation ---
    private fun startTipRotation() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                tipIndex = (tipIndex + 1) % tips.size

                val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 500 }
                val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 500 }

                tvTip.startAnimation(fadeOut)
                fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        tvTip.text = tips[tipIndex]
                        tvTip.startAnimation(fadeIn)
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })

                handler.postDelayed(this, tipInterval)
            }
        }, tipInterval)
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
            try {
                cpSkin.setProgressCompat(value, true)
            } catch (t: Throwable) {
                try {
                    cpSkin.progress = value
                } catch (_: Throwable) { }
            }
        }
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // stop tip rotation safely
    }
}

/** Simple data class and adapter **/
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

package com.example.dermahealth.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R
import com.example.dermahealth.adapter.RoutineAdapter
import com.example.dermahealth.data.Routine
import com.example.dermahealth.data.RoutineType
import com.example.dermahealth.databinding.FragmentHomeBinding
import com.example.dermahealth.helper.BackHandler
import com.example.dermahealth.helper.SwipeToDeleteCallback
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class HomeFragment : Fragment(), BackHandler {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // --- UI elements (dashboard + overlay) ---
    private lateinit var cpSkin: CircularProgressIndicator
    private lateinit var tvScore: TextView
    private lateinit var rvRoutines: RecyclerView
    private lateinit var btnAddRoutine: ImageButton
    private lateinit var tvTip: TextView
    private lateinit var ivTipIcon: ImageView
    private lateinit var nestedScroll: NestedScrollView
    private var fabScrollDown: FloatingActionButton? = null

    private lateinit var overlay: FrameLayout
    private lateinit var blurBackground: View
    private lateinit var card: MaterialCardView

    // overlay inputs
    private lateinit var etName: EditText
    private lateinit var spinnerRoutineType: Spinner
    private lateinit var etTimePicker: EditText
    private lateinit var etIntervalHours: EditText
    private lateinit var etIntervalDays: EditText
    private lateinit var etSpecificDate: EditText
    private lateinit var etComment: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var tvTitle: TextView

    // environment progress bars
    private lateinit var pbUv: ProgressBar
    private lateinit var pbHumidity: ProgressBar
    private lateinit var pbPollution: ProgressBar

    // routine list & adapter
    private val routineList = mutableListOf<Routine>()
    private lateinit var adapter: RoutineAdapter

    // temp state for inputs
    private var selectedRoutineType: RoutineType = RoutineType.DAILY
    private var selectedHour: Int = 8
    private var selectedMinute: Int = 0
    private var intervalHoursInput: Int = 1
    private var selectedIntervalDays: Int = 1
    private var selectedDateTimestamp: Long? = null

    // edit mode
    private var isEditing = false
    private var editingRoutineId: Int? = null

    // tip rotation
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

    // lifecycle helpers
    private var refreshJob: Job? = null
    private var isActive = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        // if you want to use findViewById style (keeps behaviour close to old code)
        nestedScroll = root.findViewById(R.id.nested_scroll)
        rvRoutines = root.findViewById(R.id.rv_routines)

        setupScrollAnimations(root)
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        isActive = false
        refreshJob?.cancel()
        fabScrollDown?.hide()
        _binding = null
    }

    private fun animateProgressBar(progressBar: ProgressBar, target: Int, duration: Long = 1200L) {
        ObjectAnimator.ofInt(progressBar, "progress", 0, target).apply {
            interpolator = DecelerateInterpolator()
            this.duration = duration
            start()
        }
    }

    private fun setupScrollAnimations(root: View) {
        val fadeUp = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_up)
        val nested = root.findViewById<NestedScrollView>(R.id.nested_scroll) ?: return

        nested.setOnScrollChangeListener { _, _, _, _, _ ->
            // animate direct children of the LinearLayout inside NestedScrollView
            val mainLinear = nested.getChildAt(0) as? ViewGroup ?: return@setOnScrollChangeListener
            for (child in mainLinear.children) {
                if (child.visibility == View.VISIBLE && child.tag != "animated" && isViewVisibleOnScreen(child)) {
                    child.startAnimation(fadeUp)
                    child.tag = "animated"
                }
            }

            // animate visible children in RecyclerView
            if (::rvRoutines.isInitialized) {
                val lm = rvRoutines.layoutManager as? LinearLayoutManager ?: return@setOnScrollChangeListener
                val first = lm.findFirstVisibleItemPosition()
                val last = lm.findLastVisibleItemPosition()
                if (first >= 0 && last >= first) {
                    for (i in first..last) {
                        val child = rvRoutines.findViewHolderForAdapterPosition(i)?.itemView
                        if (child != null && child.tag != "animated" && isViewVisibleOnScreen(child)) {
                            child.startAnimation(fadeUp)
                            child.tag = "animated"
                        }
                    }
                }
            }
        }

        // initial trigger
        nested.post {
            nested.scrollTo(0, 0)
            nested.scrollBy(0, 1)
        }
    }

    private fun isViewVisibleOnScreen(view: View): Boolean {
        val visible = Rect()
        val isVisible = view.getGlobalVisibleRect(visible)
        val heightVisible = visible.height()
        return isVisible && (heightVisible >= view.height / 2)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Progress Bars (Right-side) ---
        pbUv = view.findViewById(R.id.pb_uv)
        pbHumidity = view.findViewById(R.id.pb_humidity)
        pbPollution = view.findViewById(R.id.pb_pollution)

        // Dummy values (replace with API results)
        val uvValue = 6       // range 0–11+
        val humidityValue = 72 // percentage
        val pollutionValue = 130 // AQI

        // --- Core Views ---
        rvRoutines = view.findViewById(R.id.rv_routines)
        btnAddRoutine = view.findViewById(R.id.btn_add_routine)
        nestedScroll = view.findViewById(R.id.nested_scroll)
        fabScrollDown = requireActivity().findViewById(R.id.fab_scroll_down)

        // Skin views
        cpSkin = view.findViewById(R.id.cp_skin)
        tvScore = view.findViewById(R.id.tv_skin_score)
        val skinScoreValue = 75 // Place holder
        animateSkinScore(skinScoreValue)

        // Tip of the Day
        tvTip = view.findViewById(R.id.tv_tip)
        ivTipIcon = view.findViewById(R.id.iv_tip_icon)

        // --- Overlay + Card (already in XML) ---
        overlay = view.findViewById(R.id.addRoutineOverlay)
        blurBackground = overlay.findViewById(R.id.blurBackground)
        card = overlay.findViewById(R.id.card_add_routine)

        // --- Overlay EditTexts and Buttons inside the card ---
        etName = overlay.findViewById(R.id.et_routine_name)
        spinnerRoutineType = overlay.findViewById(R.id.spinner_routine_type)
        etTimePicker = overlay.findViewById(R.id.et_time_picker)
        etIntervalHours = overlay.findViewById(R.id.et_interval_hours)
        etIntervalDays = overlay.findViewById(R.id.et_interval_days)
        etSpecificDate = overlay.findViewById(R.id.et_specific_date)
        etComment = overlay.findViewById(R.id.et_routine_comment)
        btnSave = overlay.findViewById(R.id.btn_save_add)
        tvTitle = overlay.findViewById(R.id.tv_add_routine_title)
        btnCancel = overlay.findViewById(R.id.btn_cancel_add)

        // --- Spinner setup ---
        val types = listOf("Hourly", "Every X hours", "Daily", "Every X days", "Specific date")
        spinnerRoutineType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        spinnerRoutineType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedRoutineType = when (position) {
                    0 -> RoutineType.HOURLY
                    1 -> RoutineType.EVERY_X_HOURS
                    2 -> RoutineType.DAILY
                    3 -> RoutineType.EVERY_X_DAYS
                    4 -> RoutineType.SPECIFIC_DATE
                    else -> RoutineType.DAILY
                }
                updateDynamicInputsVisibility()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // --- Time picker for daily & every-x-days ---
        etTimePicker.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                etTimePicker.setText(String.format("%02d:%02d", hourOfDay, minute))
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }

        // --- Date picker for specific date (includes time) ---
        etSpecificDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val dateCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                }
                TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                    dateCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    dateCal.set(Calendar.MINUTE, minute)
                    selectedDateTimestamp = dateCal.timeInMillis
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    etSpecificDate.setText(sdf.format(Date(selectedDateTimestamp!!)))
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // --- Add Routine FAB ---
        btnAddRoutine.setOnClickListener { enterAddMode() }

        // --- Cancel button inside overlay ---
        btnCancel.setOnClickListener { hideAddRoutineCard() }

        // --- Save button (handles add & edit) ---
        btnSave.setOnClickListener {
            val title = etName.text.toString().trim()
            val note = etComment.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            intervalHoursInput = etIntervalHours.text.toString().toIntOrNull() ?: 1
            selectedIntervalDays = etIntervalDays.text.toString().toIntOrNull() ?: 1

            val routine = when (selectedRoutineType) {
                RoutineType.HOURLY -> Routine(
                    id = if (isEditing) editingRoutineId ?: generateId() else generateId(),
                    title = title,
                    type = RoutineType.HOURLY,
                    note = if (note.isBlank()) null else note
                )
                RoutineType.EVERY_X_HOURS -> Routine(
                    id = if (isEditing) editingRoutineId ?: generateId() else generateId(),
                    title = title,
                    type = RoutineType.EVERY_X_HOURS,
                    intervalHours = intervalHoursInput,
                    note = if (note.isBlank()) null else note
                )
                RoutineType.DAILY -> Routine(
                    id = if (isEditing) editingRoutineId ?: generateId() else generateId(),
                    title = title,
                    type = RoutineType.DAILY,
                    hour = selectedHour,
                    minute = selectedMinute,
                    note = if (note.isBlank()) null else note
                )
                RoutineType.EVERY_X_DAYS -> Routine(
                    id = if (isEditing) editingRoutineId ?: generateId() else generateId(),
                    title = title,
                    type = RoutineType.EVERY_X_DAYS,
                    hour = selectedHour,
                    minute = selectedMinute,
                    intervalDays = selectedIntervalDays,
                    note = if (note.isBlank()) null else note
                )
                RoutineType.SPECIFIC_DATE -> Routine(
                    id = if (isEditing) editingRoutineId ?: generateId() else generateId(),
                    title = title,
                    type = RoutineType.SPECIFIC_DATE,
                    specificDate = selectedDateTimestamp,
                    note = if (note.isBlank()) null else note
                )
            }

            if (isEditing) {
                val idx = routineList.indexOfFirst { it.id == routine.id }
                if (idx >= 0) {
                    routineList[idx] = routine
                    adapter.notifyItemChanged(idx)
                } else {
                    routineList.add(0, routine)
                    adapter.notifyItemInserted(0)
                }
            } else {
                routineList.add(routine)
            }

            sortRoutines()
            adapter.notifyDataSetChanged()
            hideAddRoutineCard()
            clearOverlayFields()
        }

        // --- Dummy routines (only once) ---
        if (routineList.isEmpty()) {
            routineList.add(Routine(1, "Apply sunscreen", RoutineType.DAILY, hour = 8, minute = 0, note = "Use SPF 50"))
            routineList.add(Routine(2, "Moisturize before bed", RoutineType.DAILY, hour = 22, minute = 0, note = "Use night cream"))
            routineList.add(Routine(3, "Drink more water", RoutineType.HOURLY, note = "At least 8 glasses"))
        }

        // --- Animate progress bars (dashboard) ---
        animateProgressBar(pbUv, uvValue)
        animateProgressBar(pbHumidity, humidityValue)
        animateProgressBar(pbPollution, pollutionValue)

        // UV text animation
        val tvUv = view.findViewById<TextView>(R.id.tv_uv_index)
        tvUv.alpha = 0f
        tvUv.text = "UV: $uvValue (High)"
        tvUv.animate().alpha(1f).setDuration(800).setStartDelay(400).start()

        // --- RecyclerView Adapter for edit/delete ---
        adapter = RoutineAdapter(
            routineList,
            onEdit = { r -> enterEditMode(r) },
            onDelete = { r -> showDeleteConfirm(r) }
        )

        rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        rvRoutines.adapter = adapter

        // --- RecyclerView swipe to delete ---
        ItemTouchHelper(SwipeToDeleteCallback(requireContext(), adapter) { position ->
            val routine = routineList[position]
            adapter.removeAt(position)
            Toast.makeText(requireContext(), "Deleted: ${routine.title}", Toast.LENGTH_SHORT).show()
        }).attachToRecyclerView(rvRoutines)

        // --- FAB scroll behaviour ---
        nestedScroll.setOnScrollChangeListener { v: NestedScrollView, _, _, _, _ ->
            val atBottom = !v.canScrollVertically(1)
            if (atBottom) fabScrollDown?.hide() else fabScrollDown?.show()
        }

        fabScrollDown?.setOnClickListener {
            nestedScroll.post {
                nestedScroll.smoothScrollTo(0, nestedScroll.getChildAt(0).bottom)
            }
        }

        // --- Tip of the Day ---
        tvTip.text = tips[tipIndex]
        startTipRotation()

        // initial visibility update & sorting
        updateDynamicInputsVisibility()
        sortRoutines()
    }

    // ---------------------------
    // UI helpers & logic
    // ---------------------------

    private fun enterAddMode() {
        isEditing = false
        editingRoutineId = null
        tvTitle.text = "Add Routine"
        clearOverlayFields()
        selectedRoutineType = RoutineType.DAILY
        spinnerRoutineType.setSelection(2) // daily
        updateDynamicInputsVisibility()
        showAddRoutineCard()
    }

    private fun enterEditMode(routine: Routine) {
        isEditing = true
        editingRoutineId = routine.id
        tvTitle.text = "Edit Routine"
        etName.setText(routine.title)
        etComment.setText(routine.note ?: "")

        selectedRoutineType = routine.type
        spinnerRoutineType.setSelection(selectedRoutineType.ordinal)

        when (routine.type) {
            RoutineType.HOURLY -> { /* nothing extra */ }
            RoutineType.EVERY_X_HOURS -> etIntervalHours.setText((routine.intervalHours ?: 1).toString())
            RoutineType.DAILY -> {
                selectedHour = routine.hour ?: 8
                selectedMinute = routine.minute ?: 0
                etTimePicker.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
            }
            RoutineType.EVERY_X_DAYS -> {
                etIntervalDays.setText((routine.intervalDays ?: 1).toString())
                selectedHour = routine.hour ?: 8
                selectedMinute = routine.minute ?: 0
                etTimePicker.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
            }
            RoutineType.SPECIFIC_DATE -> {
                routine.specificDate?.let {
                    selectedDateTimestamp = it
                    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    etSpecificDate.setText(sdf.format(Date(it)))
                }
            }
        }

        updateDynamicInputsVisibility()
        showAddRoutineCard()
    }

    private fun updateDynamicInputsVisibility() {
        etTimePicker.visibility = View.GONE
        etIntervalHours.visibility = View.GONE
        etIntervalDays.visibility = View.GONE
        etSpecificDate.visibility = View.GONE

        when (selectedRoutineType) {
            RoutineType.HOURLY -> {}
            RoutineType.EVERY_X_HOURS -> etIntervalHours.visibility = View.VISIBLE
            RoutineType.DAILY -> etTimePicker.visibility = View.VISIBLE
            RoutineType.EVERY_X_DAYS -> {
                etIntervalDays.visibility = View.VISIBLE
                etTimePicker.visibility = View.VISIBLE
            }
            RoutineType.SPECIFIC_DATE -> etSpecificDate.visibility = View.VISIBLE
        }
    }

    private fun clearOverlayFields() {
        etName.text.clear()
        etTimePicker.text.clear()
        etIntervalHours.text.clear()
        etIntervalDays.text.clear()
        etSpecificDate.text.clear()
        etComment.text.clear()
        selectedDateTimestamp = null
        selectedHour = 8
        selectedMinute = 0
        intervalHoursInput = 1
        selectedIntervalDays = 1
    }

    private fun showAddRoutineCard() {
        overlay.visibility = View.VISIBLE
        card.visibility = View.VISIBLE
        overlay.bringToFront()

        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(250).start()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurRadius = 20f
            val renderEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
            blurBackground.setRenderEffect(renderEffect)
            blurBackground.alpha = 1f
        } else {
            blurBackground.alpha = 1f
        }

        card.translationY = 300f
        card.alpha = 0f
        card.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

     fun hideAddRoutineCard() {
        card.animate()
            .translationY(300f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                overlay.animate().alpha(0f).setDuration(200).withEndAction {
                    overlay.visibility = View.GONE
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        blurBackground.setRenderEffect(null)
                    }
                    isEditing = false
                    editingRoutineId = null
                }.start()
            }
            .start()
    }

    private fun generateId(): Int = (routineList.maxOfOrNull { it.id } ?: 0) + 1

    private fun sortRoutines() {
        routineList.sortWith(compareBy<Routine> { it.type.priority }.thenBy {
            when {
                it.type == RoutineType.SPECIFIC_DATE && it.specificDate != null -> it.specificDate!!
                it.hour != null && it.minute != null -> (it.hour!! * 60 + it.minute!!).toLong()
                else -> Int.MAX_VALUE.toLong()
            }
        })
    }

    private fun showDeleteConfirm(routine: Routine) {
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

    override fun onBackPressed(): Boolean {
        return if (overlay.isVisible) {
            hideAddRoutineCard()
            true
        } else false
    }

    // Tip rotation with fade animation
    private fun startTipRotation() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                tipIndex = (tipIndex + 1) % tips.size

                val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 500 }
                val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 500 }

                tvTip.startAnimation(fadeOut)
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        tvTip.text = tips[tipIndex]
                        tvTip.startAnimation(fadeIn)
                    }
                    override fun onAnimationRepeat(animation: Animation?) {}
                })

                handler.postDelayed(this, tipInterval)
            }
        }, tipInterval)
    }

    fun isOverlayVisible(): Boolean {
        return overlay.visibility == View.VISIBLE
    }
    // Skin score animation helper
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
}

//import kotlinx.coroutines.*
//import androidx.lifecycle.lifecycleScope
//import android.animation.ValueAnimator
//import android.app.AlertDialog
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import com.example.dermahealth.helper.BackHandler
//import androidx.core.view.isVisible
//import android.view.animation.AlphaAnimation
//import android.view.animation.Animation
//import android.widget.ImageButton
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.google.android.material.progressindicator.CircularProgressIndicator
//import android.graphics.Rect
//import android.util.DisplayMetrics
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.view.animation.AnimationUtils
//import android.view.animation.DecelerateInterpolator
//import androidx.core.view.children
//import androidx.core.widget.NestedScrollView
//import com.google.android.material.floatingactionbutton.FloatingActionButton
//import kotlin.math.roundToInt
//import android.animation.ObjectAnimator
//import android.app.TimePickerDialog
//import android.graphics.RenderEffect
//import android.graphics.Shader
//import android.os.Build
//import android.util.Log
//import android.widget.Button
//import android.widget.EditText
//import android.widget.FrameLayout
//import android.widget.ProgressBar
//import android.widget.Spinner
//import com.example.dermahealth.adapter.ProductAdapter
//import com.example.dermahealth.adapter.RoutineAdapter
//import com.example.dermahealth.data.Routine
//import com.example.dermahealth.databinding.FragmentHomeBinding
//import androidx.recyclerview.widget.ItemTouchHelper
//import com.example.dermahealth.data.RoutineType
//import com.example.dermahealth.helper.SwipeToDeleteCallback
//import com.google.android.material.card.MaterialCardView
//import kotlinx.coroutines.Job
//import java.util.Calendar
//
//class HomeFragment : Fragment(), BackHandler {
//
//    private lateinit var cpSkin: CircularProgressIndicator
//    private lateinit var tvScore: TextView
//    private lateinit var rvRoutines: RecyclerView
//    private lateinit var btnAddRoutine: ImageButton
//    private lateinit var tvTip: TextView
//    private lateinit var ivTipIcon: ImageView
//    private val routineList = mutableListOf<Routine>()
//    private lateinit var adapter: RoutineAdapter
//    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
//    private var fabScrollDown: FloatingActionButton? = null
//    private var _binding: FragmentHomeBinding? = null
//    private val binding get() = _binding!!
//    private var refreshJob: Job? = null
//    private var isActive = true
//    private lateinit var overlay: FrameLayout
//    private lateinit var blurBackground: View
//    private lateinit var card: MaterialCardView
//    // Class-level lateinit properties at the top of HomeFragment
//    private lateinit var etName: EditText
//    private lateinit var etTime: EditText
//    private lateinit var etComment: EditText
//    private lateinit var btnSave: Button
//    private lateinit var tvTitle: TextView
//    private lateinit var btnCancel: Button
//    // Routine Type Selector
//    private lateinit var spinnerRoutineType: Spinner
//    private var selectedRoutineType: RoutineType = RoutineType.DAILY
//
//    // Time selection
//    private var selectedHour: Int = 8
//    private var selectedMinute: Int = 0
//
//    // Interval inputs
//    private var intervalHoursInput: Int = 1
//    private var selectedIntervalDays: Int = 1
//
//    // Specific date
//    private var selectedDateTimestamp: Long? = null
//
//
//    // --- Tip of the Day rotation ---
//    private val tips = listOf(
//        "90% of skin aging is caused by sun exposure — wear sunscreen daily.",
//        "Drink at least 8 glasses of water a day to keep your skin hydrated.",
//        "A balanced diet rich in antioxidants helps your skin glow naturally.",
//        "Cleanse your face gently, avoid over-scrubbing.",
//        "Sleep is your skin’s best friend — aim for 7–8 hours."
//    )
//    private var tipIndex = 0
//    private val handler = Handler(Looper.getMainLooper())
//    private val tipInterval = 5000L // 5 seconds
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentHomeBinding.inflate(inflater, container, false)
//        val root = binding.root
//
//        // keep your existing findViewById code (if views aren’t yet migrated to binding)
//        nestedScroll = root.findViewById(R.id.nested_scroll)
//        rvRoutines = root.findViewById(R.id.rv_routines)
//
//        setupScrollAnimations(root)
//        return root
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        handler.removeCallbacksAndMessages(null) // stop tip rotation
//        isActive = false
//        refreshJob?.cancel()
//        fabScrollDown?.hide()
//        _binding = null // ✅ safely clear binding
//    }
//
//    private fun dpToPx(dp: Int): Int {
//        val density = resources.displayMetrics.density
//        return (dp * density).roundToInt()
//    }
//
//    private fun animateProgressBar(progressBar: ProgressBar, target: Int, duration: Long = 1200L) {
//        ObjectAnimator.ofInt(progressBar, "progress", 0, target).apply {
//            interpolator = DecelerateInterpolator()
//            this.duration = duration
//            start()
//        }
//    }
//    private fun setupScrollAnimations(root: View) {
//        val fadeUp = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_up)
//        val nested = root.findViewById<NestedScrollView>(R.id.nested_scroll) ?: return
//
//        nested.setOnScrollChangeListener { _, _, _, _, _ ->
//            // animate direct children of the LinearLayout inside NestedScrollView
//            val mainLinear = nested.getChildAt(0) as? ViewGroup ?: return@setOnScrollChangeListener
//            for (child in mainLinear.children) {
//                if (child.visibility == View.VISIBLE && child.tag != "animated" && isViewVisibleOnScreen(child)) {
//                    child.startAnimation(fadeUp)
//                    child.tag = "animated"
//                }
//            }
//
//            // animate visible children in RecyclerView
//            if (::rvRoutines.isInitialized) {
//                val lm = rvRoutines.layoutManager as? LinearLayoutManager ?: return@setOnScrollChangeListener
//                val first = lm.findFirstVisibleItemPosition()
//                val last = lm.findLastVisibleItemPosition()
//                if (first >= 0 && last >= first) {
//                    for (i in first..last) {
//                        val child = rvRoutines.findViewHolderForAdapterPosition(i)?.itemView
//                        if (child != null && child.tag != "animated" && isViewVisibleOnScreen(child)) {
//                            child.startAnimation(fadeUp)
//                            child.tag = "animated"
//                        }
//                    }
//                }
//            }
//        }
//
//        // initial trigger
//        nested.post {
//            nested.scrollTo(0, 0)
//            nested.scrollBy(0, 1)
//        }
//    }
//
//    private fun isViewVisibleOnScreen(view: View): Boolean {
//        val visible = Rect()
//        val isVisible = view.getGlobalVisibleRect(visible)
//        // require at least half of the view to be visible
//        val heightVisible = visible.height()
//        return isVisible && (heightVisible >= view.height / 2)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        // --- Progress Bars (Right-side) ---
//        val pbUv = view.findViewById<ProgressBar>(R.id.pb_uv)
//        val pbHumidity = view.findViewById<ProgressBar>(R.id.pb_humidity)
//        val pbPollution = view.findViewById<ProgressBar>(R.id.pb_pollution)
//
//        // Dummy values (replace with API results)
//        val uvValue = 6       // range 0–11+
//        val humidityValue = 72 // percentage
//        val pollutionValue = 130 // AQI
//
//        // --- Core Views ---
//        rvRoutines = view.findViewById(R.id.rv_routines)
//        btnAddRoutine = view.findViewById(R.id.btn_add_routine)
//        nestedScroll = view.findViewById(R.id.nested_scroll)
//        fabScrollDown = requireActivity().findViewById(R.id.fab_scroll_down)
//
//        // Skin views
//        cpSkin = view.findViewById(R.id.cp_skin)
//        tvScore = view.findViewById(R.id.tv_skin_score)
//
//        // Tip of the Day
//        tvTip = view.findViewById(R.id.tv_tip)
//        ivTipIcon = view.findViewById(R.id.iv_tip_icon)
//
//        // --- Overlay + Card (already in XML) ---
//        overlay = view.findViewById(R.id.addRoutineOverlay)
//        blurBackground = overlay.findViewById(R.id.blurBackground)
//        card = overlay.findViewById(R.id.card_add_routine)
//
//        // --- EditTexts and Buttons inside the card ---
//        etName = overlay.findViewById(R.id.et_routine_name)
//        etTime = overlay.findViewById(R.id.et_routine_time)
//        etComment = overlay.findViewById(R.id.et_routine_comment)
//        btnSave = overlay.findViewById(R.id.btn_save_add)
//        tvTitle = overlay.findViewById(R.id.tv_add_routine_title)
//        btnCancel = overlay.findViewById(R.id.btn_cancel_add)
//
//        // --- Add Routine FAB ---
//        btnAddRoutine.setOnClickListener {
//            tvTitle.text = "Add Routine"       // Reset title
//            etName.text.clear()
//            etTime.text.clear()
//            etComment.text.clear()
//            showAddRoutineCard()
//
//            // btnSave for adding
//            btnSave.setOnClickListener {
//                val title = etName.text.toString().trim()
//                val note = etComment.text.toString().trim()
//                val type = selectedRoutineType  // you get this from dropdown
//
//                if (title.isEmpty()) {
//                    Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show()
//                    return@setOnClickListener
//                }
//
//                val newRoutine = when (type) {
//                    RoutineType.HOURLY -> Routine(
//                        id = generateId(),
//                        title = title,
//                        type = RoutineType.HOURLY,
//                        note = note
//                    )
//
//                    RoutineType.EVERY_X_HOURS -> Routine(
//                        id = generateId(),
//                        title = title,
//                        type = RoutineType.EVERY_X_HOURS,
//                        intervalHours = intervalHoursInput,
//                        note = note
//                    )
//
//                    RoutineType.DAILY -> Routine(
//                        id = generateId(),
//                        title = title,
//                        type = RoutineType.DAILY,
//                        hour = selectedHour,
//                        minute = selectedMinute,
//                        note = note
//                    )
//
//                    RoutineType.EVERY_X_DAYS -> Routine(
//                        id = generateId(),
//                        title = title,
//                        type = RoutineType.EVERY_X_DAYS,
//                        hour = selectedHour,
//                        minute = selectedMinute,
//                        intervalDays = selectedIntervalDays,
//                        note = note
//                    )
//
//                    RoutineType.SPECIFIC_DATE -> Routine(
//                        id = generateId(),
//                        title = title,
//                        type = RoutineType.SPECIFIC_DATE,
//                        specificDate = selectedDateTimestamp,
//                        note = note
//                    )
//                }
//
//                routineList.add(newRoutine)
//                sortRoutines()
//                hideAddRoutineCard()
//            }
//
//        }
//
//        // --- Cancel button ---
//        btnCancel.setOnClickListener {
//            hideAddRoutineCard()
//        }
//
//        // --- Save button ---
//        btnSave.setOnClickListener {
//            val title = etName.text.toString().trim()
//            val time = etTime.text.toString().trim()
//            val comment = etComment.text.toString().trim() // <-- additional comment
//
//            if (title.isEmpty() || time.isEmpty()) {
//                Toast.makeText(requireContext(), "Please fill in title and time", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val id = (routineList.maxOfOrNull { it.id } ?: 0) + 1
//            val newRoutine = Routine(id, title, time, note = if (comment.isBlank()) null else comment)
//            routineList.add(0, newRoutine)
//            adapter.notifyItemInserted(0)
//            rvRoutines.scrollToPosition(0)
//
//            Toast.makeText(requireContext(), "Routine added", Toast.LENGTH_SHORT).show()
//            hideAddRoutineCard()
//
//            // Clear inputs
//            etName.text.clear()
//            etTime.text.clear()
//            etComment.text.clear()
//        }
//
//
//        Log.d("DEBUG_OVERLAY", "overlay: $overlay")
//        Log.d("DEBUG_OVERLAY", "card: $card")
//        Log.d("DEBUG_OVERLAY", "etName: $etName, etTime: $etTime, etComment: $etComment")
//        Log.d("DEBUG_OVERLAY", "btnCancel: $btnCancel, btnSave: $btnSave")
//
//        // --- Dummy routines (only once) ---
//        if (routineList.isEmpty()) {
//            routineList.add(Routine(1, "Apply sunscreen", "08:00 AM", "Use SPF 50"))
//            routineList.add(Routine(2, "Moisturize before bed", "10:00 PM", "Use night cream"))
//            routineList.add(Routine(3, "Drink more water", "Throughout the day", "At least 8 glasses"))
//        }
//
//        // --- TimePicker listener (use same etTime) ---
//        etTime.setOnClickListener {
//            val cal = Calendar.getInstance()
//            val hour = cal.get(Calendar.HOUR_OF_DAY)
//            val minute = cal.get(Calendar.MINUTE)
//
//            // Use TimePickerDialog from Material 2
//            val timePicker = TimePickerDialog(
//                requireContext(),
//                R.style.CustomTimePickerDialog,
//                { _, selectedHour, selectedMinute ->
//                    val formatted = String.format("%02d.%02d", selectedHour, selectedMinute)
//                    etTime.setText(formatted)
//                },
//                hour,
//                minute,
//                true
//            )
//            timePicker.show()
//        }
//
//
//        // Animate progress bars
//        animateProgressBar(pbUv, uvValue)
//        animateProgressBar(pbHumidity, humidityValue)
//        animateProgressBar(pbPollution, pollutionValue)
//
//        // UV text animation
//        val tvUv = view.findViewById<TextView>(R.id.tv_uv_index)
//        tvUv.alpha = 0f
//        tvUv.text = "UV: $uvValue (High)"
//        tvUv.animate().alpha(1f).setDuration(800).setStartDelay(400).start()
//
//        // --- Scroll & FAB setup ---
//        nestedScroll.setOnScrollChangeListener { v: NestedScrollView, _, _, _, _ ->
//            val atBottom = !v.canScrollVertically(1)
//            if (atBottom) fabScrollDown?.hide() else fabScrollDown?.show()
//        }
//
//        fabScrollDown?.setOnClickListener {
//            nestedScroll.post {
//                nestedScroll.smoothScrollTo(0, nestedScroll.getChildAt(0).bottom)
//            }
//        }
//
//        // --- Tip of the Day ---
//        tvTip.text = tips[tipIndex]
//        startTipRotation()
//
//        // --- RecyclerView Adapter for edit/delete ---
//        adapter = RoutineAdapter(
//            routineList,
//            onEdit = { routine ->
//                // Populate overlay fields
//                tvTitle.text = "Edit Routine"
//                etName.setText(routine.title)
//                etTime.setText(routine.time)
//                etComment.setText(routine.note ?: "")
//
//                showAddRoutineCard()
//
//                spinnerRoutineType = overlay.findViewById(R.id.spinner_routine_type)
//
//                val types = listOf(
//                    "Hourly",
//                    "Every X hours",
//                    "Daily",
//                    "Every X days",
//                    "Specific date"
//                )
//
//                spinnerRoutineType.adapter = ArrayAdapter(
//                    requireContext(),
//                    android.R.layout.simple_spinner_dropdown_item,
//                    types
//                )
//
//// Listen for selection
//                spinnerRoutineType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                    override fun onItemSelected(
//                        parent: AdapterView<*>,
//                        view: View?,
//                        position: Int,
//                        id: Long
//                    ) {
//                        selectedRoutineType = when (position) {
//                            0 -> RoutineType.HOURLY
//                            1 -> RoutineType.EVERY_X_HOURS
//                            2 -> RoutineType.DAILY
//                            3 -> RoutineType.EVERY_X_DAYS
//                            4 -> RoutineType.SPECIFIC_DATE
//                            else -> RoutineType.DAILY
//                        }
//                    }
//
//                    override fun onNothingSelected(parent: AdapterView<*>) {}
//                }
//
//                etTime.setOnClickListener {
//                    val cal = Calendar.getInstance()
//                    TimePickerDialog(requireContext(), { _, hour, minute ->
//                        selectedHour = hour
//                        selectedMinute = minute
//                        etTime.setText(String.format("%02d:%02d", hour, minute))
//                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
//                }
//
//                // btnSave for editing
//                btnSave.setOnClickListener {
//                    val title = etName.text.toString().trim()
//                    val note = etComment.text.toString().trim()
//
//                    if (title.isEmpty()) {
//                        Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show()
//                        return@setOnClickListener
//                    }
//
//                    val newRoutine = when (selectedRoutineType) {
//                        RoutineType.HOURLY -> Routine(
//                            id = generateId(),
//                            title = title,
//                            type = RoutineType.HOURLY,
//                            note = note
//                        )
//
//                        RoutineType.EVERY_X_HOURS -> Routine(
//                            id = generateId(),
//                            title = title,
//                            type = RoutineType.EVERY_X_HOURS,
//                            intervalHours = intervalHoursInput,
//                            note = note
//                        )
//
//                        RoutineType.DAILY -> Routine(
//                            id = generateId(),
//                            title = title,
//                            type = RoutineType.DAILY,
//                            hour = selectedHour,
//                            minute = selectedMinute,
//                            note = note
//                        )
//
//                        RoutineType.EVERY_X_DAYS -> Routine(
//                            id = generateId(),
//                            title = title,
//                            type = RoutineType.EVERY_X_DAYS,
//                            hour = selectedHour,
//                            minute = selectedMinute,
//                            intervalDays = selectedIntervalDays,
//                            note = note
//                        )
//
//                        RoutineType.SPECIFIC_DATE -> Routine(
//                            id = generateId(),
//                            title = title,
//                            type = RoutineType.SPECIFIC_DATE,
//                            specificDate = selectedDateTimestamp,
//                            note = note
//                        )
//                    }
//
//                    routineList.add(newRoutine)
//                    sortRoutines()
//                    adapter.notifyDataSetChanged()
//                    hideAddRoutineCard()
//                }
//
//            },
//            onDelete = { routine ->
//                showDeleteConfirm(routine, adapter)
//            }
//        )
//
//        // --- RecyclerView setup ---
//        rvRoutines.layoutManager = LinearLayoutManager(requireContext())
//        rvRoutines.adapter = adapter
//
//        // --- RecyclerView swipe to delete ---
//        val swipeToDeleteCallback = SwipeToDeleteCallback(requireContext(), adapter) { position ->
//            val routine = routineList[position]
//            adapter.removeAt(position)
//            Toast.makeText(requireContext(), "Deleted: ${routine.title}", Toast.LENGTH_SHORT).show()
//        }
//        ItemTouchHelper(swipeToDeleteCallback).attachToRecyclerView(rvRoutines)
//
//        // --- Animate skin score ---
//        animateSkinScore(85)
//    }
//
//    override fun onBackPressed(): Boolean {
//        return if (overlay.isVisible) {
//            hideAddRoutineCard() // close overlay
//            true
//        } else {
//            false
//        }
//    }
//
//    // --- Tip rotation with fade animation ---
//    private fun startTipRotation() {
//        handler.postDelayed(object : Runnable {
//            override fun run() {
//                tipIndex = (tipIndex + 1) % tips.size
//
//                val fadeOut = AlphaAnimation(1f, 0f).apply { duration = 500 }
//                val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 500 }
//
//                tvTip.startAnimation(fadeOut)
//                fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
//                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
//                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
//                        tvTip.text = tips[tipIndex]
//                        tvTip.startAnimation(fadeIn)
//                    }
//                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
//                })
//
//                handler.postDelayed(this, tipInterval)
//            }
//        }, tipInterval)
//    }
//    private fun showDeleteConfirm(routine: Routine, adapter: RoutineAdapter) {
//        AlertDialog.Builder(requireContext())
//            .setMessage(getString(R.string.delete_confirm))
//            .setPositiveButton(getString(R.string.delete)) { _, _ ->
//                val index = routineList.indexOfFirst { it.id == routine.id }
//                if (index >= 0) {
//                    routineList.removeAt(index)
//                    adapter.notifyItemRemoved(index)
//                    Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
//                }
//            }
//            .setNegativeButton(getString(R.string.cancel), null)
//            .show()
//    }
//
//    private fun animateSkinScore(target: Int) {
//        val animator = ValueAnimator.ofInt(0, target)
//        animator.duration = 800
//        animator.addUpdateListener { valueAnimator ->
//            val value = valueAnimator.animatedValue as Int
//            tvScore.text = value.toString()
//            try {
//                cpSkin.setProgressCompat(value, true)
//            } catch (t: Throwable) {
//                try {
//                    cpSkin.progress = value
//                } catch (_: Throwable) { }
//            }
//        }
//        animator.start()
//    }
//
//    // ---- Add these functions at fragment level ----
//    private fun showAddRoutineCard() {
//        overlay.visibility = View.VISIBLE
//        card.visibility = View.VISIBLE
//        overlay.bringToFront()
//
//        overlay.alpha = 0f
//        overlay.animate().alpha(1f).setDuration(250).start()
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            val blurRadius = 20f
//            val renderEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP)
//            blurBackground.setRenderEffect(renderEffect)
//            blurBackground.alpha = 1f
//        } else {
//            blurBackground.alpha = 1f
//        }
//
//        card.translationY = 300f
//        card.alpha = 0f
//        card.animate()
//            .translationY(0f)
//            .alpha(1f)
//            .setDuration(300)
//            .setInterpolator(DecelerateInterpolator())
//            .start()
//    }
//
//    private fun generateId(): Int {
//        return (routineList.maxOfOrNull { it.id } ?: 0) + 1
//    }
//
//    fun hideAddRoutineCard() {
//        card.animate()
//            .translationY(300f)
//            .alpha(0f)
//            .setDuration(200)
//            .withEndAction {
//                overlay.animate().alpha(0f).setDuration(200).withEndAction {
//                    overlay.visibility = View.GONE
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                        blurBackground.setRenderEffect(null)
//                    }
//                }.start()
//            }
//            .start()
//    }
//
//
//    // Add this function to HomeFragment
//    fun closeOverlayIfVisible(): Boolean {
//        return if (overlay.visibility == View.VISIBLE) {
//            hideAddRoutineCard()
//            true
//        } else false
//    }
//
//    private fun showTimePicker(view: View) {
//        val cal = Calendar.getInstance()
//        val hour = cal.get(Calendar.HOUR_OF_DAY)
//        val minute = cal.get(Calendar.MINUTE)
//
//        TimePickerDialog(view.context, { _, h, m ->
//            val formatted = String.format("%02d.%02d", h, m)
//            (view as EditText).setText(formatted)
//        }, hour, minute, true).show()
//    }
//
//    private fun sortRoutines() {
//        routineList.sortWith(compareBy<Routine> {
//            it.type.priority
//        }.thenBy {
//            // Secondary sort: time (if applicable)
//            when {
//                it.hour != null && it.minute != null ->
//                    it.hour!! * 60 + it.minute!!
//                else -> Int.MAX_VALUE
//            }
//        })
//
//        adapter.notifyDataSetChanged()
//    }
//
//
//
//
//    override fun onResume() {
//        super.onResume() // start auto-scroll
//    }
//
//    override fun onPause() {
//        super.onPause()
//        refreshJob?.cancel()
//    }
//}
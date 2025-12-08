package com.example.dermahealth.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.Context
import com.google.gson.reflect.TypeToken
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dermahealth.R
import com.example.dermahealth.adapter.RoutineAdapter
import com.example.dermahealth.data.Routine
import com.example.dermahealth.data.RoutineType
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.databinding.FragmentHomeBinding
import com.example.dermahealth.helper.BackHandler
import com.example.dermahealth.helper.SwipeToDeleteCallback
import com.example.dermahealth.viewmodel.SharedViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.Job
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.getValue
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
    private lateinit var tvNoRoutines: TextView

    private lateinit var overlay: FrameLayout
    private lateinit var blurBackground: View
    private lateinit var card: MaterialCardView

    // overlay inputs
    private lateinit var etName: EditText
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
    private lateinit var tvUv: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvPollution: TextView


    // routine list & adapter
    private val routineList = mutableListOf<Routine>()
    private lateinit var adapter: RoutineAdapter               // RecyclerView adapter
    private lateinit var spinnerRoutineType: MaterialAutoCompleteTextView  // AutoCompleteTextView for routine type


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

    // database fetch
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

    // --- Stats TextViews ---
    private lateinit var tvTotalScans: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tvGreeting: TextView
    private val sharedViewModel: SharedViewModel by activityViewModels()

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (checkLocationPermission()) {
            getUserLocation()
        } else {
            //requestLocationPermission()
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // --- RecyclerView Adapter for edit/delete ---
        // Adapter for RecyclerView
        adapter = RoutineAdapter(
            routineList,
            onEdit = { r -> enterEditMode(r) },
            onDelete = { r -> showDeleteConfirm(r) }
        )

        loadUserGreeting()
        loadRoutineList()

        // --- Progress Bars (Right-side) ---
        pbUv = view.findViewById(R.id.pb_uv)
        pbHumidity = view.findViewById(R.id.pb_humidity)
        pbPollution = view.findViewById(R.id.pb_pollution)

        tvUv = view.findViewById(R.id.tv_uv_index)
        tvHumidity = view.findViewById(R.id.tv_humidity)
        tvPollution = view.findViewById(R.id.tv_pollution)

        // --- Core Views ---
        rvRoutines = view.findViewById(R.id.rv_routines)
        btnAddRoutine = view.findViewById(R.id.btn_add_routine)
        nestedScroll = view.findViewById(R.id.nested_scroll)
        fabScrollDown = requireActivity().findViewById(R.id.fab_scroll_down)

        // Skin views
        cpSkin = view.findViewById(R.id.cp_skin)
        tvScore = view.findViewById(R.id.tv_skin_score)

        // Total scans & detected categories
        tvTotalScans = view.findViewById(R.id.tv_total_scans)
        tvGreeting = view.findViewById(R.id.tv_greeting)

        tvNoRoutines = view.findViewById(R.id.tv_no_routines)


        sharedViewModel.history.observe(viewLifecycleOwner) { historyList ->
            if (historyList.isNotEmpty()) {

                val newScore = updateOverallSkinScore(historyList)

                // Save to Firestore so all fragments always have latest score
                db.collection("statistics")
                    .document(auth.currentUser!!.uid)
                    .update("overallSkinScore", newScore)

                // You may optionally animate local UI too:
                animateSkinScore(newScore)
            }
        }

        // Fetch and display score from Firestore
        fetchOverallSkinScore()

        // Tip of the Day
        tvTip = view.findViewById(R.id.tv_tip)
        ivTipIcon = view.findViewById(R.id.iv_tip_icon)

        // --- Overlay + Card (already in XML) ---
        overlay = view.findViewById(R.id.addRoutineOverlay)
        blurBackground = overlay.findViewById(R.id.blurBackground)
        card = overlay.findViewById(R.id.card_add_routine)

        // --- Overlay EditTexts and Buttons inside the card ---
        etName = overlay.findViewById(R.id.et_routine_name)
        etTimePicker = overlay.findViewById(R.id.et_time_picker)
        etIntervalHours = overlay.findViewById(R.id.et_interval_hours)
        etIntervalDays = overlay.findViewById(R.id.et_interval_days)
        etSpecificDate = overlay.findViewById(R.id.et_specific_date)
        etComment = overlay.findViewById(R.id.et_routine_comment)
        btnSave = overlay.findViewById(R.id.btn_save_add)
        tvTitle = overlay.findViewById(R.id.tv_add_routine_title)
        btnCancel = overlay.findViewById(R.id.btn_cancel_add)

        // spinnerRoutineType = overlay.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_routine_type)

        val layoutRoutineType = overlay.findViewById<TextInputLayout>(R.id.layout_routine_type)
        spinnerRoutineType = overlay.findViewById<MaterialAutoCompleteTextView>(R.id.actv_routine_type)  // <- correctly assigned

        val types = listOf(
            "Hourly",
            "Hourly (Specific Time)",
            "Every X Hours",
            "Every X Hours (Specific Time)",
            "Daily",
            "Every X Days",
            "Specific Date"
        )

        val routineTypeNames = RoutineType.values().map { routineType ->
            when (routineType) {
                RoutineType.HOURLY -> "Hourly"
                RoutineType.HOURLY_SPECIFIC_TIME -> "Hourly (Specific Time)"
                RoutineType.EVERY_X_HOURS -> "Every X Hours"
                RoutineType.SPECIFIC_TIME_ONLY -> "Every X Hours (Specific Time)"
                RoutineType.DAILY -> "Daily"
                RoutineType.EVERY_X_DAYS -> "Every X Days"
                RoutineType.SPECIFIC_DATE -> "Specific Date"
            }
        }

        // Adapter for AutoCompleteTextView dropdown
        val routineTypeAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, routineTypeNames)
        spinnerRoutineType.setAdapter(routineTypeAdapter)  // styled dropdown


//        spinnerRoutineType.setOnItemClickListener { _, _, position, _ ->
//            selectedRoutineType = when (position) {
//                0 -> RoutineType.HOURLY
//                1 -> RoutineType.HOURLY_SPECIFIC_TIME
//                2 -> RoutineType.EVERY_X_HOURS
//                3 -> RoutineType.SPECIFIC_TIME_ONLY
//                4 -> RoutineType.DAILY
//                5 -> RoutineType.EVERY_X_DAYS
//                6 -> RoutineType.SPECIFIC_DATE
//                else -> RoutineType.DAILY
//            }
//
//            updateDynamicInputsVisibility()
//        }

        spinnerRoutineType.setOnItemClickListener { _, _, position, _ ->
            layoutRoutineType.hint = "" // hide placeholder
            selectedRoutineType = RoutineType.values()[position]
            updateDynamicInputsVisibility()
        }

        spinnerRoutineType.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    layoutRoutineType.hint = "Routine Type"
                }
            }
        })



// --- Time picker for Daily & Every-X-Days ---
        etTimePicker.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                etTimePicker.setText(String.format("%02d:%02d", hourOfDay, minute))
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }

// --- Date picker for Specific Date ---
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

        // --- Add Routine Button ---
        btnAddRoutine.setOnClickListener { enterAddMode() }

        // --- Cancel Overlay ---
        btnCancel.setOnClickListener { hideAddRoutineCard() }

        // CLICK LISTENER FOR ADD ROUTINE
        btnAddRoutine.setOnClickListener {
            showAddRoutineOverlay()
        }

        // --- Save button (handles add & edit) ---
        btnSave.setOnClickListener {
            if (isEditing) {
                updateRoutine()
                updateRoutinePlaceholder()
            } else {
                saveRoutine()
            }
        }

        // --- Dummy routines (only once) ---
//        if (routineList.isEmpty()) {
//            routineList.add(Routine(1, "Apply sunscreen", RoutineType.DAILY, hour = 8, minute = 0, note = "Use SPF 50"))
//            routineList.add(Routine(2, "Moisturize before bed", RoutineType.DAILY, hour = 22, minute = 0, note = "Use night cream"))
//            routineList.add(Routine(3, "Drink more water", RoutineType.HOURLY, note = "At least 8 glasses"))
//        }

        rvRoutines.layoutManager = LinearLayoutManager(requireContext())
        rvRoutines.adapter = adapter

        // --- RecyclerView swipe to delete ---
// --- RecyclerView swipe to delete ---
        ItemTouchHelper(
            SwipeToDeleteCallback(requireContext(), adapter) { position ->
                val routine = routineList[position]

                // Remove from list
                routineList.removeAt(position)

                // Notify adapter
                adapter.notifyItemRemoved(position)

                updateRoutinePlaceholder()
                // Save updated list to persist deletion
                saveRoutineList()

                // Optional: show Toast
                Toast.makeText(requireContext(), "Deleted: ${routine.title}", Toast.LENGTH_SHORT).show()
            }
        ).attachToRecyclerView(rvRoutines)



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

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }


    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getUserLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }


    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getUserLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getUserLocation() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    fetchEnvironmentData(latitude, longitude) // call your API function
                } else {
                    Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRoutinePlaceholder() {
        if (routineList.isEmpty()) {
            rvRoutines.visibility = View.GONE
            tvNoRoutines.visibility = View.VISIBLE
        } else {
            rvRoutines.visibility = View.VISIBLE
            tvNoRoutines.visibility = View.GONE
        }
    }


    // ---------------------------
    // UI helpers & logic
    // ---------------------------
    private fun updateOverallSkinScore(historyList: List<ScanHistory>): Int {
        if (historyList.isEmpty()) {
            animateSkinScore(0)
            return 0
        }

        val perCardAverages = historyList.map { scan ->
            val scores = scan.images.mapNotNull { it.score }
            if (scores.isNotEmpty()) {
                val avg = scores.sum() / scores.size
                if (avg <= 1f) avg * 100f else avg
            } else 0f
        }

        val overallScore = perCardAverages.sum() / perCardAverages.size

        return overallScore.toInt()
    }


    private fun animateProgressBarValue(progressBar: ProgressBar, target: Int) {
        progressBar.post {
            val animator = ValueAnimator.ofInt(progressBar.progress, target)
            animator.duration = 800
            animator.addUpdateListener { valueAnimator ->
                progressBar.progress = valueAnimator.animatedValue as Int
            }
            animator.start()
        }
    }


    // ---------------------------
    // Fetch environment data
    // ---------------------------
    private fun initEnvironmentData() {
        if (checkLocationPermission()) {
            getUserLocation()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchEnvironmentData(lat: Double, lon: Double) {
        val weatherApiKey = getString(R.string.openweather_api_key)
        val uvApiKey = getString(R.string.openuv_api_key)

        val client = OkHttpClient()

        // --- OpenWeatherMap Weather (humidity) ---
        val weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$weatherApiKey&units=metric"
        client.newCall(Request.Builder().url(weatherUrl).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = e.printStackTrace()

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val humidity = JSONObject(body).getJSONObject("main").getInt("humidity")

                    // Ensure this runs on the UI thread
                    pbHumidity.post {
                        animateProgressBarValue(pbHumidity, humidity)
                        tvHumidity.text = "Humidity: $humidity%"
                    }
                }
            }
        })


        // --- OpenWeatherMap Air Pollution (AQI) ---
        val aqiUrl = "https://api.openweathermap.org/data/2.5/air_pollution?lat=$lat&lon=$lon&appid=$weatherApiKey"
        client.newCall(Request.Builder().url(aqiUrl).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = e.printStackTrace()
            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val aqiValue = JSONObject(body)
                        .getJSONArray("list")
                        .getJSONObject(0)
                        .getJSONObject("main")
                        .getInt("aqi") // 1-5

                    val aqiLabel = when(aqiValue) {
                        1 -> "Good"
                        2 -> "Fair"
                        3 -> "Moderate"
                        4 -> "Poor"
                        5 -> "Very Poor"
                        else -> "Unknown"
                    }
                    // Optional: still animate a progress bar if you want (scaled)
                    val scaledAqi = (aqiValue / 5.0 * 500).toInt()
                    animateProgressBarValue(pbPollution, scaledAqi)
                    pbPollution.post {
                        animateProgressBarValue(pbPollution, scaledAqi)
                        tvPollution.text = "AQI: $aqiLabel"
                    }
                }
            }
        })

        // --- OpenUV (UV index) ---
        val uvUrl = "https://api.openuv.io/api/v1/uv?lat=$lat&lng=$lon"
        val uvRequest = Request.Builder()
            .url(uvUrl)
            .addHeader("x-access-token", uvApiKey)
            .build()

        client.newCall(uvRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val uvIndex = JSONObject(body)
                        .getJSONObject("result")
                        .getDouble("uv")

                    // Scale UV to 0-100 for the progress bar (max UV ~11)
                    val scaledUv = ((uvIndex / 11.0) * 100).toInt()

// UV
                    pbUv.post {
                        animateProgressBarValue(pbUv, scaledUv)
                        tvUv.text = "UV: %.1f".format(uvIndex)
                    }
                }
            }
        })

    }

    private fun loadUserGreeting() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val fullname = doc?.getString("fullName") ?: ""
                val firstName = fullname.split(" ").firstOrNull() ?: ""

                // Get current hour in device's timezone
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)

                val greeting = when (hour) {
                    in 5..11 -> "Good morning"
                    in 12..16 -> "Good afternoon"
                    in 17..20 -> "Good evening"
                    else -> "Good night"
                }

                tvGreeting.text = "$greeting, $firstName!"
            }
            .addOnFailureListener {
                tvGreeting.text = "Hello!"
            }
    }


    private fun animateSkinScore(target: Int) {
        val animator = ValueAnimator.ofInt(cpSkin.progress, target)
        animator.duration = 800
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            tvScore.text = "$value%"
            try {
                cpSkin.setProgressCompat(value, true)
            } catch (t: Throwable) {
                try { cpSkin.progress = value } catch (_: Throwable) { }
            }
        }
        animator.start()
    }

    private fun showAddRoutineOverlay() {
        overlay.visibility = View.VISIBLE
        blurBackground.visibility = View.VISIBLE
    }

    private fun Fragment.runIfSafe(block: () -> Unit) {
        if (isAdded && view != null) {
            activity?.runOnUiThread {
                block()
            }
        }
    }

    private fun saveRoutine() {

        val title = etName.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = etComment.text.toString().trim()

        val routine = Routine(
            id = generateId(),
            title = title,
            type = selectedRoutineType,
            note = comment
        )

        when (selectedRoutineType) {

            RoutineType.HOURLY -> {
                // nothing else needed
            }

            RoutineType.HOURLY_SPECIFIC_TIME -> {
                routine.hour = selectedHour
                routine.minute = selectedMinute
            }

            RoutineType.EVERY_X_HOURS -> {
                val interval = etIntervalHours.text.toString().toIntOrNull() ?: 1
                routine.intervalHours = interval
            }

            RoutineType.SPECIFIC_TIME_ONLY -> {
                routine.hour = selectedHour
                routine.minute = selectedMinute
                routine.intervalHours = null
            }


            RoutineType.DAILY -> {
                routine.hour = selectedHour
                routine.minute = selectedMinute
            }

            RoutineType.EVERY_X_DAYS -> {
                val days = etIntervalDays.text.toString().toIntOrNull() ?: 1
                routine.intervalDays = days
                routine.hour = selectedHour
                routine.minute = selectedMinute
            }

            RoutineType.SPECIFIC_DATE -> {
                if (selectedDateTimestamp == null) {
                    Toast.makeText(requireContext(), "Please select a date/time", Toast.LENGTH_SHORT).show()
                    return
                }
                routine.specificDate = selectedDateTimestamp
            }
        }

        // add to list
        routineList.add(routine)

        // sort using your priority + custom logic
        sortRoutines()

        // refresh list
        adapter.notifyDataSetChanged()

        Toast.makeText(requireContext(), "Routine added!", Toast.LENGTH_SHORT).show()
        saveRoutineList()
        hideAddRoutineCard()
    }

    private fun updateRoutine() {
        val index = routineList.indexOfFirst { it.id == editingRoutineId }
        if (index == -1) return

        val title = etName.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = etComment.text.toString().trim()

        val r = routineList[index]
        r.title = title
        r.note = comment
        r.type = selectedRoutineType

        when (selectedRoutineType) {

            RoutineType.HOURLY -> {
                r.hour = null
                r.minute = null
                r.intervalHours = null
                r.intervalDays = null
                r.specificDate = null
            }

            RoutineType.HOURLY_SPECIFIC_TIME -> {
                r.hour = selectedHour
                r.minute = selectedMinute
                r.intervalHours = null
                r.intervalDays = null
                r.specificDate = null
            }

            RoutineType.EVERY_X_HOURS -> {
                r.intervalHours = etIntervalHours.text.toString().toIntOrNull() ?: 1
                r.hour = null
                r.minute = null
                r.intervalDays = null
                r.specificDate = null
            }

            RoutineType.SPECIFIC_TIME_ONLY -> {
                r.hour = selectedHour
                r.minute = selectedMinute
                r.intervalHours = null
                r.intervalDays = null
                r.specificDate = null
            }


            RoutineType.DAILY -> {
                r.hour = selectedHour
                r.minute = selectedMinute
                r.intervalHours = null
                r.intervalDays = null
                r.specificDate = null
            }

            RoutineType.EVERY_X_DAYS -> {
                r.intervalDays = etIntervalDays.text.toString().toIntOrNull() ?: 1
                r.hour = selectedHour
                r.minute = selectedMinute
                r.intervalHours = null
                r.specificDate = null
            }

            RoutineType.SPECIFIC_DATE -> {
                r.specificDate = selectedDateTimestamp
                r.hour = null
                r.minute = null
                r.intervalHours = null
                r.intervalDays = null
            }
        }

        sortRoutines()
        adapter.notifyDataSetChanged()

        Toast.makeText(requireContext(), "Routine updated!", Toast.LENGTH_SHORT).show()
        updateRoutinePlaceholder()
        saveRoutineList()
        hideAddRoutineCard()
    }


    private fun fetchOverallSkinScore() {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid

        db.collection("statistics").document(uid)
            .addSnapshotListener { doc, error ->
                if (error != null) return@addSnapshotListener
                if (doc != null && doc.exists()) {
                    val overallScore = (doc.getDouble("overallSkinScore") ?: 0.0).toInt()
                    animateSkinScore(overallScore)
                }
            }
    }

    private fun enterAddMode() {
        isEditing = false
        editingRoutineId = null
        tvTitle.text = "Add Routine"
        clearOverlayFields()
        selectedRoutineType = RoutineType.DAILY
        spinnerRoutineType.setText("Daily", false) // <-- safe pre-selection
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
        val spinnerRoutineType = overlay.findViewById<MaterialAutoCompleteTextView>(R.id.actv_routine_type)
        spinnerRoutineType.setText(selectedRoutineType.displayName, false)

        when (routine.type) {
            RoutineType.HOURLY -> {
                // nothing extra
            }

            RoutineType.HOURLY_SPECIFIC_TIME -> {
                selectedHour = routine.hour ?: 8
                selectedMinute = routine.minute ?: 0
                etTimePicker.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
            }

            RoutineType.EVERY_X_HOURS -> {
                etIntervalHours.setText((routine.intervalHours ?: 1).toString())
            }

            RoutineType.SPECIFIC_TIME_ONLY -> {
                selectedHour = routine.hour ?: 8
                selectedMinute = routine.minute ?: 0
                etTimePicker.setText(String.format("%02d:%02d", selectedHour, selectedMinute))
            }


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

            RoutineType.HOURLY -> {
                etTimePicker.visibility = View.GONE
                etIntervalHours.visibility = View.GONE
                etIntervalDays.visibility = View.GONE
                etSpecificDate.visibility = View.GONE
            }

            RoutineType.HOURLY_SPECIFIC_TIME -> {
                etTimePicker.visibility = View.VISIBLE
                etIntervalHours.visibility = View.GONE
                etIntervalDays.visibility = View.GONE
                etSpecificDate.visibility = View.GONE
            }

            RoutineType.EVERY_X_HOURS -> {
                etIntervalHours.visibility = View.VISIBLE
                etTimePicker.visibility = View.GONE
                etIntervalDays.visibility = View.GONE
                etSpecificDate.visibility = View.GONE
            }

            RoutineType.SPECIFIC_TIME_ONLY -> {
                etIntervalHours.visibility = View.GONE
                etTimePicker.visibility = View.VISIBLE
                etIntervalDays.visibility = View.GONE
                etSpecificDate.visibility = View.GONE
            }

            RoutineType.DAILY -> {
                etTimePicker.visibility = View.VISIBLE
                etIntervalHours.visibility = View.GONE
                etIntervalDays.visibility = View.GONE
                etSpecificDate.visibility = View.GONE
            }

            RoutineType.EVERY_X_DAYS -> {
                etTimePicker.visibility = View.VISIBLE
                etIntervalHours.visibility = View.GONE
                etIntervalDays.visibility = View.VISIBLE
                etSpecificDate.visibility = View.GONE
            }

            RoutineType.SPECIFIC_DATE -> {
                etSpecificDate.visibility = View.VISIBLE
                etTimePicker.visibility = View.GONE
                etIntervalHours.visibility = View.GONE
                etIntervalDays.visibility = View.GONE
            }
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

    private fun saveRoutineList() {
        val prefs = requireContext().getSharedPreferences("routine_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(routineList)
        editor.putString("routine_list", json)
        editor.apply()
    }

    private fun loadRoutineList() {
        val prefs = requireContext().getSharedPreferences("routine_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString("routine_list", null)

        if (json != null) {
            val type = object : TypeToken<MutableList<Routine>>() {}.type
            val loadedList: MutableList<Routine> = Gson().fromJson(json, type)

            routineList.clear()
            routineList.addAll(loadedList)
        }
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

}
package com.example.dermahealth

import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.graphics.Rect
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.core.view.children
import androidx.core.widget.NestedScrollView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlin.math.roundToInt
import android.animation.ObjectAnimator
import android.util.Log
import android.widget.ProgressBar
import androidx.recyclerview.widget.GridLayoutManager
import com.example.dermahealth.adapter.ProductAdapter
import com.example.dermahealth.api.RetrofitInstanceMakeup
import com.example.dermahealth.api.RetrofitInstanceOBF
import com.example.dermahealth.data.Product
import com.example.dermahealth.databinding.FragmentHomeBinding
import com.example.dermahealth.model.MakeupProduct
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var cpSkin: CircularProgressIndicator
    private lateinit var tvScore: TextView
    private lateinit var rvRoutines: RecyclerView
    private lateinit var btnAddRoutine: ImageButton
    private lateinit var tvTip: TextView
    private lateinit var ivTipIcon: ImageView
    private val routineList = mutableListOf<Routine>()
    private lateinit var adapter: RoutineAdapter
    private lateinit var vpCarousel: ViewPager2
    private lateinit var nestedScroll: androidx.core.widget.NestedScrollView
    private var currentPage = 0
    private val carouselHandler = Handler(Looper.getMainLooper())
    private lateinit var carouselRunnable: Runnable
    private var fabScrollDown: FloatingActionButton? = null
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var rvProducts: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private var refreshJob: Job? = null
    private val refreshIntervalMs = 20000L // 15 seconds
    private var isActive = true
    private var makeupRefreshJob: Job? = null
    private val makeupRefreshIntervalMs = 20000L // 15 seconds
    private lateinit var rvMakeup: RecyclerView
    private lateinit var makeupAdapter: ProductAdapter

    // Put your image resource ids here (or URLs if you load remotely with Glide/Picasso)
    private val carouselImages: List<Int> = listOf(
        R.drawable.carousel_1,
        R.drawable.carousel_2,
        R.drawable.carousel_3
    )

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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root = binding.root

        // keep your existing findViewById code (if views aren’t yet migrated to binding)
        vpCarousel = root.findViewById(R.id.vp_carousel)
        nestedScroll = root.findViewById(R.id.nested_scroll)
        rvRoutines = root.findViewById(R.id.rv_routines)

        // your original functionality
        setupCarousel()
        setupScrollAnimations(root)
        fetchProductsPeriodically()
        fetchMakeupProductsPeriodically()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // stop tip rotation
        isActive = false
        refreshJob?.cancel()
        fabScrollDown?.hide()
        _binding = null // ✅ safely clear binding
    }

    private fun setupCarousel() {
        // Adapter
        vpCarousel.adapter = CarouselAdapter(carouselImages)

        // Auto-scroll every 3 seconds
        carouselRunnable = Runnable {
            if (vpCarousel.adapter != null) {
                val itemCount = vpCarousel.adapter!!.itemCount
                currentPage = (vpCarousel.currentItem + 1) % itemCount
                vpCarousel.setCurrentItem(currentPage, true)
            }
            carouselHandler.postDelayed(carouselRunnable, 10000) // 3 sec
        }

        // disable overscroll glow
        vpCarousel.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER

        // Page transformer for margin + scale
        val transformer = CompositePageTransformer()
        transformer.addTransformer(MarginPageTransformer(dpToPx(12)))
        transformer.addTransformer { page, position ->
            // subtle scale so the centered page is bigger
            val r = 1 - kotlin.math.abs(position)
            page.scaleY = 0.95f + (r * 0.05f)
        }
        vpCarousel.setPageTransformer(transformer)

        // Set the exact height of the ViewPager2 content at runtime:
        // width = screenWidth * 0.36 (≈ 1/3 + a little). Height = width * 0.65 (adjust ratio to taste).
        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidthPx = displayMetrics.widthPixels
        val itemWidthPx = (screenWidthPx * 0.45f).roundToInt() // adjust image width here
        val itemHeightPx = (itemWidthPx * 1.65f).roundToInt() // adjust image height here

        // Set ViewPager2 height so each page fits nicely
        val lp = vpCarousel.layoutParams
        lp.height = itemHeightPx + dpToPx(12) // add a little for padding/margin
        vpCarousel.layoutParams = lp

        // Make pages peek (by using offscreenPageLimit and padding on ViewPager2)
        vpCarousel.offscreenPageLimit = 3

        // 🔹 Slow down swipe animation duration (default ~250ms → set to ~800ms)
        try {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(vpCarousel) as RecyclerView

            val scrollerField = RecyclerView::class.java.getDeclaredField("mViewFlinger")
            scrollerField.isAccessible = true
            val viewFlinger = scrollerField.get(recyclerView)

            val interpolator = DecelerateInterpolator()
            val scroller = object : Scroller(vpCarousel.context, interpolator) {
                override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
                    // Force our custom duration
                    super.startScroll(startX, startY, dx, dy, 800) // 800ms for smoother swipe
                }
            }

            val scrollerObjField = viewFlinger!!::class.java.getDeclaredField("mScroller")
            scrollerObjField.isAccessible = true
            scrollerObjField.set(viewFlinger, scroller)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).roundToInt()
    }

    private fun animateProgressBar(progressBar: ProgressBar, target: Int, duration: Long = 1200L) {
        ObjectAnimator.ofInt(progressBar, "progress", 0, target).apply {
            interpolator = DecelerateInterpolator()
            this.duration = duration
            start()
        }
    }
    private fun setupScrollAnimations(root: View) {
        // load animation
        val fadeUp = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_up)

        // if nestedScroll not found by id, fallback find
        val nsv = root as? androidx.core.widget.NestedScrollView ?: root.findViewById(android.R.id.content)

        // We will monitor scroll changes on NestedScrollView and animate children when they become visible.
        val nested = if (nsv is androidx.core.widget.NestedScrollView) nsv else nestedScroll

        nested.setOnScrollChangeListener { _, _, _, _, _ ->
            // animate direct children of the LinearLayout inside nested scroll (the main column)
            val mainLinear = nested.getChildAt(0) as? ViewGroup ?: return@setOnScrollChangeListener
            for (child in mainLinear.children) {
                if (child.visibility == View.VISIBLE && child.tag != "animated" && isViewVisibleOnScreen(child)) {
                    child.startAnimation(fadeUp)
                    child.tag = "animated" // prevent reanimation if you don't want repeated animations
                }
            }

            // animate visible children in RecyclerView (for items)
            if (::rvRoutines.isInitialized) {
                val layoutManager = rvRoutines.layoutManager ?: return@setOnScrollChangeListener
                val first = (rvRoutines.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.findFirstVisibleItemPosition() ?: -1
                val last = (rvRoutines.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.findLastVisibleItemPosition() ?: -1
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

        // initial trigger so top content animates on first draw
        nested.post {
            nested.scrollTo(0, 0)
            nested.scrollBy(0, 1) // tiny scroll to fire listener
        }
    }

    private fun isViewVisibleOnScreen(view: View): Boolean {
        val visible = Rect()
        val isVisible = view.getGlobalVisibleRect(visible)
        // require at least half of the view to be visible
        val heightVisible = visible.height()
        return isVisible && (heightVisible >= view.height / 2)
    }

    // --- Carousel Adapter ---
    inner class CarouselAdapter(private val images: List<Int>) : RecyclerView.Adapter<CarouselAdapter.CarouselVH>() {
        inner class CarouselVH(item: View) : RecyclerView.ViewHolder(item) {
            val iv: ImageView = item.findViewById(R.id.iv_carousel_item)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_carousel, parent, false)
            return CarouselVH(view)
        }

        override fun onBindViewHolder(holder: CarouselVH, position: Int) {
            val idx = position % images.size
            holder.iv.setImageResource(images[idx])
        }

        override fun getItemCount(): Int = Int.MAX_VALUE // makes carousel virtually infinite; careful with very large scrolls
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Right-side progress bar (UV, Humidity, Pollution)

        val pbUv = view.findViewById<ProgressBar>(R.id.pb_uv)
        val pbHumidity = view.findViewById<ProgressBar>(R.id.pb_humidity)
        val pbPollution = view.findViewById<ProgressBar>(R.id.pb_pollution)

        // Dummy values (replace with API results)
        val uvValue = 6       // range 0–11+
        val humidityValue = 72 // percentage
        val pollutionValue = 130 // AQI

        // Animate progress
        animateProgressBar(pbUv, uvValue)
        animateProgressBar(pbHumidity, humidityValue)
        animateProgressBar(pbPollution, pollutionValue)

        val tvUv = view.findViewById<TextView>(R.id.tv_uv_index)
        tvUv.alpha = 0f
        tvUv.text = "UV: $uvValue (High)"
        tvUv.animate().alpha(1f).setDuration(800).setStartDelay(400).start()

        // === Scroll & FAB setup ===
        nestedScroll = view.findViewById(R.id.nested_scroll)
        fabScrollDown = requireActivity().findViewById(R.id.fab_scroll_down)

        nestedScroll.setOnScrollChangeListener { v: NestedScrollView, _, _, _, _ ->
            val atBottom = !v.canScrollVertically(1)
            if (atBottom) fabScrollDown?.hide() else fabScrollDown?.show()
        }

        fabScrollDown?.setOnClickListener {
            nestedScroll.post {
                nestedScroll.smoothScrollTo(0, nestedScroll.getChildAt(0).bottom)
            }
        }

        // Skin setup
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
        startAutoRefresh()
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

//    private fun setupProductGrid(products: List<Product>) {
//        val adapter = ProductAdapter(products)
//        val layoutManager = GridLayoutManager(requireContext(), 2)
//        binding.rvProducts.layoutManager = layoutManager
//        binding.rvProducts.adapter = adapter
//    }

    private fun setupProductsRecyclerView(products: List<Product>) {
        rvProducts = binding.rvProducts
        productAdapter = ProductAdapter(products)
        rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        rvProducts.adapter = productAdapter
    }

    private fun setupMakeupRecycler(products: List<Product>) {
        rvMakeup = binding.rvMakeup
        makeupAdapter = ProductAdapter(products)
        rvMakeup.layoutManager = GridLayoutManager(requireContext(), 2)
        rvMakeup.adapter = makeupAdapter
    }

    private fun fetchProductsPeriodically() {
        refreshJob?.cancel() // cancel previous if any

        refreshJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    // Fetch next page
                    val response = RetrofitInstanceOBF.api.getProducts(page = currentPage, pageSize = 10)

                    // Convert API response to local Product model
                    val convertedProducts = response.products.map {
                        Product(
                            name = it.name,
                            brand = it.brand,
                            imageUrl = it.imageUrl
                        )
                    }

                    // Filter and format valid ones
                    val filteredProducts = convertedProducts
                        .filter { product ->
                            val name = product.name?.trim().orEmpty()
                            val validName = name.isNotEmpty() &&
                                    !name.matches(Regex("^\\d+$")) &&
                                    name.lowercase() != "null"
                            val hasImage = !product.imageUrl.isNullOrEmpty()
                            validName && hasImage
                        }
                        .map { product ->
                            val formattedName = product.name?.lowercase()
                                ?.split(" ")
                                ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                ?: "Unnamed Product"

                            val formattedBrand = product.brand?.lowercase()
                                ?.split(" ")
                                ?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                ?: "Unknown Brand"

                            product.copy(name = formattedName, brand = formattedBrand)
                        }

                    // ✅ Limit to 4 valid products only
                    val limitedProducts = filteredProducts.shuffled().take(4)

                    if (limitedProducts.isNotEmpty()) {
                        crossfadeRecyclerView {
                            setupProductsRecyclerView(limitedProducts)
                        }
                    }

                    Log.d("OBF", "Page $currentPage fetched: ${limitedProducts.size} products shown")

                    // Move to next page (loop around if needed)
                    currentPage++
                    if (currentPage > 50) currentPage = 1

                    delay(refreshIntervalMs)

                } catch (e: Exception) {
                    Log.e("OBF", "Error fetching products: ${e.message}")
                    delay(5000) // retry slower on error
                }
            }
        }
    }

    // 🔹 List of product types available from Makeup API
    private val makeupCategories = listOf(
        "blush", "bronzer", "eyebrow", "eyeliner", "eyeshadow",
        "foundation", "lip_liner", "lipstick", "mascara", "nail_polish"
    )

    private fun fetchMakeupProductsPeriodically() {
        makeupRefreshJob?.cancel()

        makeupRefreshJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    val selectedTypes = makeupCategories.shuffled().take(4)

                    val allProducts = selectedTypes.map { type ->
                        async {
                            try {
                                val response = RetrofitInstanceMakeup.api.getProducts(productType = type)
                                val validItems = response.mapNotNull {
                                    val imageUrl = it.imageUrl?.trim()
                                    val name = it.name?.trim().orEmpty()
                                    val brand = it.brand?.trim().orEmpty()

                                    // ✅ Filter: must have image and valid name
                                    if (
                                        imageUrl.isNullOrEmpty() ||
                                        !imageUrl.matches(Regex("(?i).+\\.(jpg|jpeg|png|webp)$")) ||
                                        name.isEmpty() ||
                                        name.equals("null", ignoreCase = true)
                                    ) return@mapNotNull null

                                    val formattedName = name.split(" ")
                                        .joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } }

                                    val formattedBrand = if (brand.isNotEmpty()) {
                                        brand.split(" ")
                                            .joinToString(" ") { w -> w.replaceFirstChar { c -> c.uppercase() } }
                                    } else {
                                        "Unknown Brand"
                                    }

                                    Product(
                                        name = formattedName,
                                        brand = formattedBrand,
                                        imageUrl = imageUrl
                                    )
                                }

                                // Return one random valid product per category if available
                                validItems.shuffled().firstOrNull()
                            } catch (e: Exception) {
                                Log.e("MAKEUP", "Error fetching $type: ${e.message}")
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()

                    if (allProducts.isNotEmpty()) {
                        crossfadeMakeupRecyclerView {
                            setupMakeupRecycler(allProducts)
                        }
                        Log.d("MAKEUP", "✅ Displayed ${allProducts.size} products from ${selectedTypes.joinToString()}")
                    } else {
                        Log.w("MAKEUP", "⚠️ No valid products found — retrying next cycle")
                    }

                    delay(makeupRefreshIntervalMs)

                } catch (e: Exception) {
                    Log.e("MAKEUP", "Error in periodic refresh: ${e.message}")
                    delay(5000)
                }
            }
        }
    }

    private fun startAutoRefresh() {
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(refreshIntervalMs)
                fetchProductsPeriodically()        // 💧 Skincare from OBF
                fetchMakeupProductsPeriodically()
            }
        }
    }

    // Function (paste inside your Fragment class)
    private fun crossfadeRecyclerView(onFadeOutComplete: () -> Unit) {
        // Use the nullable backing binding to avoid NPEs if view was destroyed
        val rv = _binding?.rvProducts ?: return

        // Fade-out animation
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 400
            fillAfter = true
        }

        // Fade-in animation
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 400
            fillAfter = true
        }

        // When fade-out ends, update data then start fade-in
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Ensure UI updates happen on main thread
                activity?.runOnUiThread {
                    // Replace adapter data / rebind UI
                    onFadeOutComplete()

                    // Start fade-in; ensure final alpha is set after animation
                    fadeIn.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            // explicit final state
                            rv.alpha = 1f
                            // remove listeners to avoid accidental repeats
                            fadeIn.setAnimationListener(null)
                        }
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                    rv.startAnimation(fadeIn)
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Start fade-out (ensure run on main)
        activity?.runOnUiThread {
            rv.startAnimation(fadeOut)
        }
    }

    private fun crossfadeMakeupRecyclerView(onFadeOutComplete: () -> Unit) {
        // Use the nullable backing binding to avoid NPEs if view was destroyed
        val rv = _binding?.rvMakeup ?: return

        // Fade-out animation
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 400
            fillAfter = true
        }

        // Fade-in animation
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 400
            fillAfter = true
        }

        // When fade-out ends, update data then start fade-in
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                // Ensure UI updates happen on main thread
                activity?.runOnUiThread {
                    // Replace adapter data / rebind UI
                    onFadeOutComplete()

                    // Start fade-in; ensure final alpha is set after animation
                    fadeIn.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            // explicit final state
                            rv.alpha = 1f
                            // remove listeners to avoid accidental repeats
                            fadeIn.setAnimationListener(null)
                        }
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                    rv.startAnimation(fadeIn)
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })

        // Start fade-out (ensure run on main)
        activity?.runOnUiThread {
            rv.startAnimation(fadeOut)
        }
    }


    override fun onResume() {
        super.onResume()
        carouselHandler.postDelayed(carouselRunnable, 10000) // start auto-scroll
    }

    override fun onPause() {
        super.onPause()
        carouselHandler.removeCallbacks(carouselRunnable) // stop auto-scroll
        refreshJob?.cancel()
        makeupRefreshJob?.cancel()
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

        // --- Animation ---
        holder.itemView.alpha = 0f
        holder.itemView.translationY = 50f

        holder.itemView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setStartDelay(position * 80L) // staggered entry
            .start()
    }

    override fun getItemCount(): Int = items.size
}

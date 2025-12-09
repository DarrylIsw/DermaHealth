package com.example.dermahealth

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.dermahealth.fragments.HistoryFragment
import com.example.dermahealth.fragments.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.children
import com.example.dermahealth.fragments.*
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var circle: View
    private var currentSelectedView: View? = null

    // Track BottomNavigationView tab history
    private val tabStack = ArrayDeque<Int>()
    private var currentTabId = R.id.nav_home // default start tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        circle = findViewById(R.id.nav_circle)

        adjustBottomNavForScreenSize()  // <-- call this here
        // --- in MainActivity ---
        val fragmentStack = ArrayDeque<Fragment>()
        var currentTabId = R.id.nav_home

// Load default fragment
        if (savedInstanceState == null) {
            val home = HomeFragment()
            fragmentStack.addLast(home)
            loadFragment(home)
            currentTabId = R.id.nav_home

            // ⚡ Ensure the circle is visible and positioned correctly
            bottomNav.post {
                val homeView = getNavItemView(R.id.nav_home)
                homeView?.let {
                    currentSelectedView = it
                    moveCircleInstant(it)
                    updateNavIcons(R.id.nav_home)
                }
            }
        }

        // BottomNav listener
        bottomNav.setOnItemSelectedListener { item ->
            val targetView = getNavItemView(item.itemId) ?: return@setOnItemSelectedListener false

            if (item.itemId != currentTabId) {
                val newFragment: Fragment = when (item.itemId) {
                    R.id.nav_home -> HomeFragment()
                    R.id.nav_scan -> ScanFragment()
                    R.id.nav_history -> HistoryFragment()
                    R.id.nav_profile -> ProfileFragment()
                    else -> return@setOnItemSelectedListener false
                }

                // Push new fragment to stack
                fragmentStack.addLast(newFragment)
                currentTabId = item.itemId
                loadFragment(newFragment)

                currentSelectedView?.let { from -> animateCircleTransition(from, targetView, circle) }
                    ?: moveCircleInstant(targetView)
                currentSelectedView = targetView
                updateNavIcons(item.itemId)
            }
            true
        }

        // Back handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = fragmentStack.lastOrNull()

                // Overlay check
                if (currentFragment is HomeFragment && currentFragment.isOverlayVisible()) {
                    currentFragment.hideAddRoutineCard()
                    return
                }

                if (fragmentStack.size > 1) {
                    fragmentStack.removeLast()
                    val previousFragment = fragmentStack.last()
                    loadFragment(previousFragment)

                    // Update BottomNav selection
                    val previousTabId = when (previousFragment) {
                        is HomeFragment -> R.id.nav_home
                        is ScanFragment -> R.id.nav_scan
                        is HistoryFragment -> R.id.nav_history
                        is ProfileFragment -> R.id.nav_profile
                        else -> R.id.nav_home
                    }
                    bottomNav.selectedItemId = previousTabId
                    currentTabId = previousTabId

                    // ⚡ Ensure circle is positioned correctly when navigating via back
                    bottomNav.post {
                        val targetView = getNavItemView(previousTabId)
                        targetView?.let { moveCircleInstant(it) }
                    }
                } else {
                    finish()
                }
            }
        })
    }

    /** Load selected fragment */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /** Update icon colors */
    private fun updateNavIcons(selectedItemId: Int) {
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val color = if (menuItem.itemId == selectedItemId)
                ContextCompat.getColor(this, android.R.color.white)
            else
                ContextCompat.getColor(this, R.color.medium_sky_blue)

            menuItem.icon?.setTint(color) // safer than setColorFilter
            menuItem.title?.let { } // title color is handled by itemTextColor
        }
    }



    /** Get BottomNavigationView item view safely */
    private fun getNavItemView(itemId: Int): View? {
        bottomNav.findViewById<View>(itemId)?.let { return it }
        val menuView = (bottomNav.getChildAt(0) as? ViewGroup) ?: return null
        for (i in 0 until menuView.childCount) {
            val child = menuView.getChildAt(i)
            if (child.findViewById<View?>(itemId) != null) return child
        }
        return null
    }

    /** Instantly move circle on first load */
    private fun moveCircleInstant(targetView: View) {
        circle.post {
            circle.visibility = View.VISIBLE
            val targetCenterX = targetView.x + targetView.width / 2f
            circle.x = targetCenterX - circle.width / 2f
        }
    }

    /** Animate circle transition (⚡ faster + smoother) */
    private fun animateCircleTransition(fromView: View, toView: View, circle: View) {
        circle.post {
            val startX = fromView.x + fromView.width / 2f - circle.width / 2f
            val endX = toView.x + toView.width / 2f - circle.width / 2f

            circle.x = startX
            circle.animate()
                .scaleX(0.8f).scaleY(0.8f)
                .setDuration(100)
                .withEndAction {
                    circle.animate()
                        .x(endX)
                        .setDuration(120)
                        .withEndAction {
                            circle.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(100)
                                .start()
                        }.start()
                }.start()
        }
    }

    private fun adjustBottomNavForScreenSize() {
        val displayMetrics = resources.displayMetrics
        val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)

        if (screenHeightDp > 800) {
            val bottomContainer = findViewById<View>(R.id.bottom_container)
            val navCircle = findViewById<View>(R.id.nav_circle)
            val navBackground = findViewById<View>(R.id.nav_background)
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

            // Set bottom nav background to certain height
            navBackground.layoutParams.height = 115.dpToPx()
            navBackground.requestLayout()

            // Set BottomNavigationView height to 90dp
            bottomNav.layoutParams.height = 90.dpToPx()
            bottomNav.requestLayout()

            bottomContainer.post {
                // Keep bottomContainer height proportional to background if needed
                val newHeight = navBackground.layoutParams.height
                bottomContainer.layoutParams.height = newHeight
                bottomContainer.requestLayout()
            }

            navCircle.post {
                val newWidth = (navCircle.measuredWidth * 1.23).toInt()
                val newHeight = (navCircle.measuredHeight * 1.23).toInt()
                navCircle.layoutParams.width = newWidth
                navCircle.layoutParams.height = newHeight
                navCircle.translationY = -15.dpToPx().toFloat() // move higher
                navCircle.requestLayout()
            }

            fragmentContainer.post {
                val bottomNavHeight = bottomContainer.measuredHeight
                // Scale fragment container height so it doesn't overlap top
                val newHeight = ((displayMetrics.heightPixels - bottomNavHeight) * 1.5).toInt()
                fragmentContainer.layoutParams.height = newHeight
                fragmentContainer.requestLayout()
            }

            bottomNav.post {
                // Iterate over menu items safely
                for (i in 0 until bottomNav.menu.size()) {
                    val menuItemView = bottomNav.findViewById<View>(
                        bottomNav.menu.getItem(i).itemId
                    )
                    // Adjust top padding for icons if needed
                    menuItemView?.setPadding(
                        menuItemView.paddingLeft,
                        (menuItemView.paddingTop - 4.dpToPx()),
                        menuItemView.paddingRight,
                        menuItemView.paddingBottom
                    )
                }
            }
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

}

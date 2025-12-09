package com.example.dermahealth

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.dermahealth.fragments.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var circle: View
    private var currentSelectedView: View? = null

    // Fragments
    private val homeFragment = HomeFragment()
    private val scanFragment = ScanFragment()
    private val historyFragment = HistoryFragment()
    private val profileFragment = ProfileFragment()
    private var activeFragment: Fragment = homeFragment

    // Track BottomNavigationView tab history
    private val tabStack = ArrayDeque<Int>()
    private var currentTabId = R.id.nav_home // default start tab

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        circle = findViewById(R.id.nav_circle)

        adjustBottomNavForScreenSize()  // Keep your current adjustments

        // Restore active fragment after process death
        activeFragment = savedInstanceState?.let {
            val tag = it.getString("active_fragment_tag")
            supportFragmentManager.findFragmentByTag(tag)
        } ?: homeFragment

        setupFragments()
        setupBottomNav()

        // Restore circle position on rotation/process restore
        bottomNav.post {
            val view = getNavItemView(currentTabId)
            view?.let { moveCircleInstant(it) }
        }

        // Back handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeFragment is HomeFragment && (activeFragment as HomeFragment).isOverlayVisible()) {
                    (activeFragment as HomeFragment).hideAddRoutineCard()
                    return
                }

                if (tabStack.size > 1) {
                    tabStack.removeLast()
                    val previousTabId = tabStack.last()
                    switchToTab(previousTabId)
                } else {
                    finish()
                }
            }
        })
    }

    private fun setupFragments() {
        val ft = supportFragmentManager.beginTransaction()
        // Add all fragments but show only activeFragment
        listOf(homeFragment, scanFragment, historyFragment, profileFragment).forEach { frag ->
            if (!frag.isAdded) {
                ft.add(R.id.fragment_container, frag, frag.javaClass.simpleName)
                if (frag != activeFragment) ft.hide(frag)
            }
        }
        ft.commit()
    }

    private fun setupBottomNav() {
        currentTabId = when (activeFragment) {
            is HomeFragment -> R.id.nav_home
            is ScanFragment -> R.id.nav_scan
            is HistoryFragment -> R.id.nav_history
            is ProfileFragment -> R.id.nav_profile
            else -> R.id.nav_home
        }
        tabStack.clear()
        tabStack.add(currentTabId)
        bottomNav.selectedItemId = currentTabId

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId != currentTabId) {
                switchToTab(item.itemId)
            }
            true
        }
    }

    private fun switchToTab(tabId: Int) {
        val fragmentToShow = when (tabId) {
            R.id.nav_home -> homeFragment
            R.id.nav_scan -> scanFragment
            R.id.nav_history -> historyFragment
            R.id.nav_profile -> profileFragment
            else -> homeFragment
        }

        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragmentToShow)
            .commit()

        activeFragment = fragmentToShow
        currentTabId = tabId
        tabStack.add(tabId)
        bottomNav.selectedItemId = tabId

        val targetView = getNavItemView(tabId)
        targetView?.let {
            currentSelectedView?.let { from -> animateCircleTransition(from, it, circle) }
                ?: moveCircleInstant(it)
            currentSelectedView = it
        }

        updateNavIcons(tabId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("active_fragment_tag", activeFragment.tag)
    }

    /** Load selected fragment (not used for bottom nav anymore, kept for other cases) */
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
            menuItem.icon?.setTint(color)
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

    /** Animate circle transition */
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

    /** Bottom nav adjustment for larger screens */
    private fun adjustBottomNavForScreenSize() {
        val displayMetrics = resources.displayMetrics
        val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
        val fragmentContainer = findViewById<FrameLayout>(R.id.fragment_container)

        if (screenHeightDp > 800) {
            val bottomContainer = findViewById<View>(R.id.bottom_container)
            val navCircle = findViewById<View>(R.id.nav_circle)
            val navBackground = findViewById<View>(R.id.nav_background)
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

            navBackground.layoutParams.height = 115.dpToPx()
            navBackground.requestLayout()

            bottomNav.layoutParams.height = 90.dpToPx()
            bottomNav.requestLayout()

            bottomContainer.post {
                val newHeight = navBackground.layoutParams.height
                bottomContainer.layoutParams.height = newHeight
                bottomContainer.requestLayout()
            }

            navCircle.post {
                val newWidth = (navCircle.measuredWidth * 1.23).toInt()
                val newHeight = (navCircle.measuredHeight * 1.23).toInt()
                navCircle.layoutParams.width = newWidth
                navCircle.layoutParams.height = newHeight
                navCircle.translationY = -15.dpToPx().toFloat()
                navCircle.requestLayout()
            }

            fragmentContainer.post {
                val bottomNavHeight = bottomContainer.measuredHeight
                val newHeight = ((displayMetrics.heightPixels - bottomNavHeight) * 1.5).toInt()
                fragmentContainer.layoutParams.height = newHeight
                fragmentContainer.requestLayout()
            }
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}

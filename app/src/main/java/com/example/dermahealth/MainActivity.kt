package com.example.dermahealth

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.dermahealth.fragments.HistoryFragment
import com.example.dermahealth.fragments.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var circle: View
    private var currentSelectedView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)
        circle = findViewById(R.id.nav_circle)

        // Load default fragment if this is the first launch
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())

            bottomNav.post {
                val homeView = getNavItemView(R.id.nav_home)
                homeView?.let {
                    currentSelectedView = it
                    moveCircleInstant(it)
                    updateNavIcons(R.id.nav_home) // set default icons
                }
            }
        }

        // Handle nav item selection
        bottomNav.setOnItemSelectedListener { item ->
            val newFragment: Fragment
            val targetView = getNavItemView(item.itemId) ?: return@setOnItemSelectedListener false

            // Switch fragments
            newFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_scan -> ScanFragment()
                R.id.nav_history -> HistoryFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> return@setOnItemSelectedListener false
            }
            loadFragment(newFragment)

            // Animate circle transition
            currentSelectedView?.let { from ->
                animateCircleTransition(from, targetView, circle)
            } ?: run {
                moveCircleInstant(targetView)
            }
            currentSelectedView = targetView

            // Update icons & backgrounds
            updateNavIcons(item.itemId)

            true
        }
    }

    /** Load the selected fragment */
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /** Tint icons + set background on selected item */
    private fun updateNavIcons(selectedItemId: Int) {
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            val view = getNavItemView(menuItem.itemId)

            if (menuItem.itemId == selectedItemId) {
                // Selected → white icon + blue bg circle
                menuItem.icon?.setColorFilter(
                    ContextCompat.getColor(this, android.R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
                view?.setBackgroundResource(R.drawable.bg_nav_circle)
            } else {
                // Unselected → medium sky blue icon, no bg
                menuItem.icon?.setColorFilter(
                    ContextCompat.getColor(this, R.color.medium_sky_blue),
                    PorterDuff.Mode.SRC_IN
                )
                view?.background = null
            }
        }
    }

    /** Safely get BottomNavigationView item view */
    private fun getNavItemView(itemId: Int): View? {
        val menuView = bottomNav.getChildAt(0) as? ViewGroup ?: return null
        for (i in 0 until menuView.childCount) {
            val child = menuView.getChildAt(i)
            if (child.id == itemId) {
                return child
            }
        }
        return null
    }

    /** Instantly place circle on first load */
    private fun moveCircleInstant(targetView: View) {
        circle.visibility = View.VISIBLE
        circle.x = targetView.x + targetView.width / 2f - circle.width / 2f
    }

    /** Shrink → Move → Expand animation */
    private fun animateCircleTransition(fromView: View, toView: View, circle: View) {
        val startX = fromView.x + fromView.width / 2f - circle.width / 2f
        val endX = toView.x + toView.width / 2f - circle.width / 2f

        circle.x = startX

        // Shrink
        circle.animate()
            .scaleX(0.2f).scaleY(0.2f)
            .setDuration(150)
            .withEndAction {
                // Move
                circle.animate()
                    .x(endX)
                    .setDuration(200)
                    .withEndAction {
                        // Expand
                        circle.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(150)
                            .start()
                    }.start()
            }.start()
    }
}

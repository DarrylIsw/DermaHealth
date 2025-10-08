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
                    updateNavIcons(R.id.nav_home)
                }
            }
        }

        // Handle nav item selection
        bottomNav.setOnItemSelectedListener { item ->
            val targetView = getNavItemView(item.itemId) ?: return@setOnItemSelectedListener false
            val newFragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_scan -> ScanFragment()
                R.id.nav_history -> HistoryFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> return@setOnItemSelectedListener false
            }

            loadFragment(newFragment)

            // ⚡ Faster circle animation transition
            currentSelectedView?.let { from ->
                animateCircleTransition(from, targetView, circle)
            } ?: moveCircleInstant(targetView)
            currentSelectedView = targetView

            updateNavIcons(item.itemId)
            true
        }
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
            if (menuItem.itemId == selectedItemId) {
                menuItem.icon?.setColorFilter(
                    ContextCompat.getColor(this, android.R.color.white),
                    PorterDuff.Mode.SRC_IN
                )
            } else {
                menuItem.icon?.setColorFilter(
                    ContextCompat.getColor(this, R.color.medium_sky_blue),
                    PorterDuff.Mode.SRC_IN
                )
            }
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
                .setDuration(100) // fast shrink
                .withEndAction {
                    circle.animate()
                        .x(endX)
                        .setDuration(120) // fast move
                        .withEndAction {
                            circle.animate()
                                .scaleX(1f).scaleY(1f)
                                .setDuration(100) // fast restore
                                .start()
                        }.start()
                }.start()
        }
    }
}

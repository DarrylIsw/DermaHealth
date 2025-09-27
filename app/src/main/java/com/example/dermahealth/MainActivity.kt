package com.example.dermahealth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.dermahealth.fragments.HomeFragment
import com.example.dermahealth.fragments.ScanFragment
import com.example.dermahealth.fragments.HistoryFragment
import com.example.dermahealth.fragments.ProfileFragment
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_nav)

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Handle nav item clicks
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_scan -> loadFragment(ScanFragment())
                R.id.nav_history -> loadFragment(HistoryFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}

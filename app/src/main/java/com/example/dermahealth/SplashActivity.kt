package com.example.dermahealth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var logoImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logoImage = findViewById(R.id.logoImage)

        logoImage.alpha = 1f
        logoImage.bringToFront()

        // 🔹 Start fade-in animation for logo
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        logoImage.startAnimation(fadeIn)

        // 🔹 Wait for 3 seconds, then move to MainActivity (no fade-out)
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2000)
    }
}

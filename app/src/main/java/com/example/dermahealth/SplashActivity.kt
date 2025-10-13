package com.example.dermahealth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var logoImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logoImage = findViewById(R.id.logoImage)

        // Bring the logo upfront
        logoImage.alpha = 1f
        logoImage.bringToFront()

        // 🔹 Fade in the logo
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1200 // fade in for 1.2s
            fillAfter = true
        }
        logoImage.startAnimation(fadeIn)

        // 🔹 Keep it fully visible for ~1s, then fade out together with bg
        Handler(Looper.getMainLooper()).postDelayed({
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 600 // fade out duration
                fillAfter = true
            }
            logoImage.startAnimation(fadeOut)

            // Start main activity slightly after fade-out
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }, 600)
        }, 1200 + 1000) // 1200 fadeIn + 1000 delay before fadeOut starts
    }
}

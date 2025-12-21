package com.example.dermahealth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var logoImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logoImage = findViewById(R.id.logoImage)

        logoImage.alpha = 1f
        logoImage.bringToFront()

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 1200
            fillAfter = true
        }
        logoImage.startAnimation(fadeIn)

        Handler(Looper.getMainLooper()).postDelayed({
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 600
                fillAfter = true
            }
            logoImage.startAnimation(fadeOut)

            Handler(Looper.getMainLooper()).postDelayed({
                val isFirebaseLoggedIn = FirebaseAuth.getInstance().currentUser != null

                val nextIntent = if (isFirebaseLoggedIn) {
                    Intent(this, MainActivity::class.java)
                } else {
                    Intent(this, LoginRegisterActivity::class.java)
                }

                startActivity(nextIntent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()

            }, 450)
        }, 1450)
    }
}

package com.example.dermahealth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dermahealth.databinding.ActivityLoginRegisterBinding
import com.example.dermahealth.fragments.LoginFragment

class LoginRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load LoginFragment only once
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .commit()
        }
    }
}

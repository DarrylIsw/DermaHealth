package com.example.dermahealth.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Retrofit instance
// Singleton object to provide a Retrofit instance for the Makeup API
object RetrofitInstanceMakeup {

    // Lazy initialization ensures Retrofit is created only when first accessed
    val api: MakeupService by lazy {
        Retrofit.Builder()
            .baseUrl("https://makeup-api.herokuapp.com/") // Base URL of the Makeup API
            .addConverterFactory(GsonConverterFactory.create()) // Convert JSON responses to Kotlin objects
            .build() // Build the Retrofit instance
            .create(MakeupService::class.java) // Create an implementation of the MakeupService interface
    }
}


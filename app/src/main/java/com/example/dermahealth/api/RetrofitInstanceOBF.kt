package com.example.dermahealth.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Singleton object to provide a Retrofit instance for the Open Beauty Facts (OBF) API
object RetrofitInstanceOBF {

    // Lazy initialization ensures Retrofit is created only when first accessed
    val api: OBFService by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openbeautyfacts.org/") // Base URL of the OBF API
            .addConverterFactory(GsonConverterFactory.create()) // Convert JSON responses to Kotlin objects
            .build() // Build the Retrofit instance
            .create(OBFService::class.java) // Create an implementation of the OBFService interface
    }
}


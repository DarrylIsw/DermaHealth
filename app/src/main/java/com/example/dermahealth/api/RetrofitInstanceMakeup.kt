package com.example.dermahealth.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Retrofit instance
object RetrofitInstanceMakeup {
    val api: MakeupService by lazy {
        Retrofit.Builder()
            .baseUrl("https://makeup-api.herokuapp.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MakeupService::class.java)
    }
}

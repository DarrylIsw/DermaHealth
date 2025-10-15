package com.example.dermahealth.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstanceOBF {
    val api: OBFService by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openbeautyfacts.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OBFService::class.java)
    }
}

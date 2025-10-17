package com.example.dermahealth.api

import retrofit2.http.GET
import retrofit2.http.Query
import com.example.dermahealth.model.MakeupProduct

interface MakeupService {

    // Retrofit GET request to fetch makeup products
    // Endpoint: "api/v1/products.json"
    @GET("api/v1/products.json")
    suspend fun getProducts(
        // Optional query parameter to filter by product type (e.g., "lipstick", "foundation")
        @Query("product_type") productType: String? = null,

        // Optional query parameter to filter by brand (e.g., "maybelline")
        @Query("brand") brand: String? = null
    ): List<MakeupProduct> // Returns a list of MakeupProduct objects
}

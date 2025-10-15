package com.example.dermahealth.api

import retrofit2.http.GET
import retrofit2.http.Query
import com.example.dermahealth.model.MakeupProduct

interface MakeupService {
    @GET("api/v1/products.json")
    suspend fun getProducts(
        @Query("product_type") productType: String? = null,
        @Query("brand") brand: String? = null
    ): List<MakeupProduct>
}

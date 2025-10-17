package com.example.dermahealth.data

import com.google.gson.annotations.SerializedName

// Represents the top-level response from the Open Beauty Facts (OBF) API
data class OBFResponse(
    @SerializedName("products") val products: List<OBFProduct> // List of products returned by the API
)

// Represents an individual product in the OBF API response
data class OBFProduct(
    @SerializedName("product_name") val name: String?, // Name of the product, may be null
    @SerializedName("brands") val brand: String?,      // Brand name(s), may be null
    @SerializedName("image_url") val imageUrl: String? // URL of the product image, may be null
)

package com.example.dermahealth.data

import com.google.gson.annotations.SerializedName

data class OBFResponse(
    @SerializedName("products") val products: List<OBFProduct>
)

data class OBFProduct(
    @SerializedName("product_name") val name: String?,
    @SerializedName("brands") val brand: String?,
    @SerializedName("image_url") val imageUrl: String?
)

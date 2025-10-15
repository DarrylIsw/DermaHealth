// data/model/MakeupProduct.kt
package com.example.dermahealth.model

import com.google.gson.annotations.SerializedName

data class MakeupProduct(
    @SerializedName("name") val name: String?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("image_link") val imageUrl: String?,
    @SerializedName("product_type") val productType: String? // added for clarity
)

// data/model/MakeupProduct.kt
package com.example.dermahealth.model

import com.google.gson.annotations.SerializedName

// Data class representing a makeup product returned by the Makeup API
data class MakeupProduct(
    @SerializedName("name") val name: String?,           // Name of the product, may be null
    @SerializedName("brand") val brand: String?,         // Brand of the product, may be null
    @SerializedName("image_link") val imageUrl: String?, // URL to the product image
    @SerializedName("product_type") val productType: String? // Type of product (e.g., lipstick, foundation)
)

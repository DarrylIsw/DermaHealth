package com.example.dermahealth.data

// Represents a single scan history entry in the app
//data class ScanHistory(
//    val id: Long,                  // Unique identifier for the scan
//    val imageUrl: String?,         // Full image URL/path; fallback to placeholder if null
//    val result: String,            // Scan result: "Benign", "Suspicious", "Malignant", etc.
//    val dateIso: String,           // ISO date string (e.g., "2025-09-24"); format for UI display
//    val notes: String,             // User or system notes about the scan
//    val score: Float? = null,               // classification confidence score
//    val additionalImages: List<String> = emptyList(), // internal storage paths
//    val croppedUrl: String? = null,// Optional cropped image URL/path
//    val isExpanded: Boolean = false// UI state: whether this item is expanded in RecyclerView
//)

data class ScanHistory(
    val id: Long,
    val mainImage: ScanImage?,
    val dateIso: String,
    val imgName: String,
    val notes: String,
    val images: List<ScanImage> = emptyList(),
    val isExpanded: Boolean = false
)

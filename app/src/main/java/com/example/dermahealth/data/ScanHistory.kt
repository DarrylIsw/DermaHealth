package com.example.dermahealth.data

// Represents a single scan history entry in the app
data class ScanHistory(
    val id: Long,                  // Unique identifier for the scan
    val imageUrl: String?,         // Full image URL/path; fallback to placeholder if null
    val result: String,            // Scan result: "Benign", "Suspicious", "Malignant", etc.
    val dateIso: String,           // ISO date string (e.g., "2025-09-24"); format for UI display
    val notes: String,             // User or system notes about the scan
    val croppedUrl: String? = null,// Optional cropped image URL/path
    val isExpanded: Boolean = false// UI state: whether this item is expanded in RecyclerView
)

package com.example.dermahealth.data

//data class ScanHistory(
//    val id: Int,
//    val imageRes: Int,
//    val result: String,
//    val date: String,
//    var notes: String
//)

data class ScanHistory(
    val id: Long,
    val imageUrl: String?,        // use URL/path; fallback to placeholder
    val result: String,           // "Benign" | "Suspicious" | "Malignant"...
    val dateIso: String,          // store ISO (e.g., 2025-09-24) and format in UI
    val notes: String,
    val croppedUrl: String? = null,
    val isExpanded: Boolean = false
)

package com.example.dermahealth.data

data class ScanImage(
    val path: String,
    val timestamp: String,
    val label: String?,
    val score: Float?
)

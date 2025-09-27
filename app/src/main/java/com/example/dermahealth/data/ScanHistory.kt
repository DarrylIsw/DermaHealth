package com.example.dermahealth.data

data class ScanHistory(
    val id: Int,
    val imageRes: Int,
    val result: String,
    val date: String,
    var notes: String
)

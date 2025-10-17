package com.example.dermahealth.data

data class Routine(
    var id: Int, // Routine Id
    var title: String, // Routine Title
    var time: String, // Routine Timestamp
    var note: String? = null // Routine note/comment
)

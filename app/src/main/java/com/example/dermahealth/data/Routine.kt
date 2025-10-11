package com.example.dermahealth.data

data class Routine(
    var id: Int,
    var title: String,
    var time: String,
    var note: String? = null
)

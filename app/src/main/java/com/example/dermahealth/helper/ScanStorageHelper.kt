package com.example.dermahealth.helper

import android.content.Context
import com.example.dermahealth.data.ScanHistory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object ScanStorageHelper {

    private const val HISTORY_FILE = "scan_history.json"

    fun saveHistory(context: Context, list: List<ScanHistory>) {
        val json = Gson().toJson(list)
        val file = File(context.filesDir, HISTORY_FILE)
        file.writeText(json)
    }

    fun loadHistory(context: Context): List<ScanHistory> {
        val file = File(context.filesDir, HISTORY_FILE)
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val type = object : TypeToken<List<ScanHistory>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }
}

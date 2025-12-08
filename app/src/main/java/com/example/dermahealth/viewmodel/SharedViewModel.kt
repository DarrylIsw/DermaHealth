package com.example.dermahealth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.dermahealth.data.ScanHistory
import com.example.dermahealth.helper.ScanStorageHelper

class SharedViewModel(app: Application) : AndroidViewModel(app) {

    private val _history = MutableLiveData<List<ScanHistory>>(emptyList())
    val history: LiveData<List<ScanHistory>> = _history

    init {
        // Load saved history on startup
        _history.value = ScanStorageHelper.loadHistory(app)
    }

    // Save current history to persistent storage
    private fun persist() {
        _history.value?.let { ScanStorageHelper.saveHistory(getApplication(), it) }
    }

    // -------------------- SCAN HISTORY MANAGEMENT --------------------

    fun addScan(scan: ScanHistory) {
        val current = _history.value?.toMutableList() ?: mutableListOf()
        current.add(0, scan) // prepend newest
        _history.value = current
        persist()
    }

    fun updateScanList(list: List<ScanHistory>) {
        _history.value = list
        persist()
    }

    fun getLatestScan(): ScanHistory? =
        _history.value?.firstOrNull()

    fun getTotalScans(): Int =
        _history.value?.size ?: 0

    fun updateScan(updatedScan: ScanHistory) {
        val newList = _history.value?.toMutableList() ?: mutableListOf()
        val index = newList.indexOfFirst { it.id == updatedScan.id }
        if (index != -1) {
            newList[index] = updatedScan
            _history.value = newList
        }

        // And save to persistent storage (SharedPreferences, JSON, Room, etc)
        persist()
    }

    fun deleteScan(scan: ScanHistory) {
        // 1. Remove from current list
        val current = _history.value?.toMutableList() ?: mutableListOf()
        current.removeAll { it.id == scan.id }

        // 2. Update LiveData
        _history.value = current

        // 3. Save updated list to persistent storage
        persist()
    }

    // -------------------- ANALYTICS / INFO --------------------

    fun getAverageScore(): Float {
        val allScores = _history.value
            ?.flatMap { it.images }
            ?.mapNotNull { it.score }
            ?: return 0f

        return if (allScores.isEmpty()) 0f else allScores.average().toFloat()
    }

    fun getLatestLabel(): String =
        _history.value?.firstOrNull()?.mainImage?.label ?: "Unknown"

}

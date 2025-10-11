package com.example.dermahealth.helper

interface BackHandler {
    /**
     * @return true if the fragment handled the back press (e.g., closed overlay), false otherwise
     */
    fun onBackPressed(): Boolean
}

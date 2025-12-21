package com.example.dermahealth

import android.content.Context

object SessionManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_IS_LOGGED = "isLoggedIn"

    fun setLoggedIn(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_LOGGED, value).apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED, false)
    }
}

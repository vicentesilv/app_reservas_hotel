package com.example.app_reservas_hotel.utils

import android.content.Context
import androidx.core.content.edit

object PrefsUtils {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_LOGGED_USERNAME = "logged_username"

    fun getLoggedUsername(context: Context): String? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.getString(KEY_LOGGED_USERNAME, null)
        } catch (_: Exception) {
            null
        }
    }

    fun clearLoggedUsername(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit { remove(KEY_LOGGED_USERNAME) }
        } catch (_: Exception) {}
    }
}

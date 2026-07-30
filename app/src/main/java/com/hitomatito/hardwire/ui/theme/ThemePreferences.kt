package com.hitomatito.hardwire.ui.theme

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

object ThemePreferences {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun getThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return try {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)!!)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }
}

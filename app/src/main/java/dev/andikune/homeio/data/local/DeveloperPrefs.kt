package dev.andikune.homeio.data.local

import android.content.Context

object DeveloperPrefs {
    private const val PREFS_NAME = "homeio_dev_prefs"
    private const val KEY_DEV_MODE = "is_dev_mode_active"

    fun isDevModeActive(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DEV_MODE, false)
    }

    fun setDevModeActive(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEV_MODE, active)
            .apply()
    }
}

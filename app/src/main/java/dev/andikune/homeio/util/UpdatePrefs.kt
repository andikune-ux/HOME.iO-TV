package dev.andikune.homeio.util

import android.content.Context

/**
 * UpdatePrefs — Menyimpan status Skip Update & Cooldown cek.
 *
 * Logika:
 *   - User klik SKIP untuk v.1.20.14
 *   - Update v.1.20.14 tidak akan muncul lagi
 *   - Ketika rilis v.1.20.15, dialog muncul lagi
 *   - Cooldown cek: 6 jam sekali (hemat GitHub API rate limit)
 */
object UpdatePrefs {

    private const val PREFS = "homeio_update_prefs"
    private const val KEY_SKIPPED_VERSION = "skipped_version"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"

    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 jam

    fun getSkippedVersion(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SKIPPED_VERSION, null)
    }

    fun setSkippedVersion(context: Context, version: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_SKIPPED_VERSION, version)
            .apply()
    }

    fun clearSkippedVersion(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_SKIPPED_VERSION)
            .apply()
    }

    fun shouldCheckUpdate(context: Context): Boolean {
        val lastCheck = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_CHECK_TIME, 0L)
        return System.currentTimeMillis() - lastCheck >= CHECK_INTERVAL_MS
    }

    fun markChecked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Filter: Apakah versi ini harus DIMUNCULKAN di dialog?
     * Force Update SELALU dimunculkan (bypass skip).
     */
    fun shouldShowDialog(context: Context, latestVersion: String, forceUpdate: Boolean): Boolean {
        if (forceUpdate) return true
        val skipped = getSkippedVersion(context)
        return skipped != latestVersion
    }
}

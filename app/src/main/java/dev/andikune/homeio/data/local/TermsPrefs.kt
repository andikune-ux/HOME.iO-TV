package dev.andikune.homeio.data.local

import android.content.Context

object TermsPrefs {
    private const val PREFS_NAME = "homeio_terms_prefs"
    private const val KEY_AGREED = "has_agreed_to_terms"

    fun isAgreed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AGREED, false)
    }

    fun setAgreed(context: Context, agreed: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AGREED, agreed).apply()
    }
}

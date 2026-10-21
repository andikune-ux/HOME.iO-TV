package dev.andikune.homeio.util

import android.content.Context

object OnboardingPrefs {
    private const val PREFS = "homeio_onboarding"
    private const val KEY_DONE = "onboarding_done"

    fun isCompleted(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, 0).getBoolean(KEY_DONE, false)

    fun markCompleted(ctx: Context) {
        ctx.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_DONE, true).apply()
    }

    fun reset(ctx: Context) {
        ctx.getSharedPreferences(PREFS, 0).edit().remove(KEY_DONE).apply()
    }
}

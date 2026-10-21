package dev.andikune.homeio.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.util.Log

object KioskManager {
    private const val TAG = "KioskManager"
    @Volatile var isMainActivityForeground: Boolean = false

    fun enableKiosk(activity: Activity) {
        try {
            val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.startLockTask()
                Log.d(TAG, "Kiosk Mode ENABLED")
            }
        } catch (e: Exception) { Log.e(TAG, "Gagal startLockTask: ${e.message}") }
    }

    fun disableKiosk(activity: Activity) {
        try {
            val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.stopLockTask()
                Log.d(TAG, "Kiosk Mode DISABLED")
            }
        } catch (e: Exception) { Log.e(TAG, "Gagal stopLockTask: ${e.message}") }
    }
}

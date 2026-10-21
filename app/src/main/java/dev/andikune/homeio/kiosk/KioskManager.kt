package dev.andikune.homeio.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.util.Log

object KioskManager {
    private const val TAG = "KioskManager"
    @Volatile var isMainActivityForeground: Boolean = false

    fun enableKiosk(activity: Activity): Boolean {
        return try {
            val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.startLockTask()
                Log.d(TAG, "Kiosk Mode ENABLED")
            }
            true
        } catch (e: Exception) {
            // FIX BUG #6: Jangan crash, return false jika gagal
            Log.e(TAG, "Gagal startLockTask (butuh Device Owner): ${e.message}")
            false
        }
    }

    fun disableKiosk(activity: Activity) {
        try {
            val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) {
                activity.stopLockTask()
                Log.d(TAG, "Kiosk Mode DISABLED")
            }
        } catch (e: Exception) { 
            Log.e(TAG, "Gagal stopLockTask: ${e.message}") 
        }
    }
}

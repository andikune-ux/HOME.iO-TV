package dev.andikune.homeio.util

import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock

object SystemUtils {
    data class MemoryInfo(
        val totalMemMb: Long,
        val freeMemMb: Long,
        val usedMemMb: Long,
        val freePercentage: Int
    )

    fun getMemoryInfo(context: Context): MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val freeMb = memInfo.availMem / (1024 * 1024)
        val usedMb = totalMb - freeMb
        val freePct = if (totalMb > 0) ((freeMb * 100) / totalMb).toInt() else 0

        return MemoryInfo(totalMb, freeMb, usedMb, freePct)
    }

    fun boostRam(context: Context): MemoryInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(0)
        val selfPkg = context.packageName

        for (pkg in packages) {
            if (pkg.packageName != selfPkg) {
                try {
                    am.killBackgroundProcesses(pkg.packageName)
                } catch (_: Exception) {}
            }
        }
        System.gc()
        return getMemoryInfo(context)
    }

    fun isTimeSynced(): Boolean {
        // Simple sanity check: system time should be after Jan 1, 2024
        val currentTime = System.currentTimeMillis()
        val minRealisticTime = 1704067200000L // 2024-01-01
        return currentTime > minRealisticTime
    }
}

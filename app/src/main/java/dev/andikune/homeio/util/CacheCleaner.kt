package dev.andikune.homeio.util

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import java.io.File

object CacheCleaner {

    private const val TAG = "CacheCleaner"

    data class BoostResult(
        val cacheFreedBytes: Long,
        val ramBefore: Long,
        val ramAfter: Long,
        val ramFreedMb: Long,
        val appsKilled: Int,
        val success: Boolean,
        val message: String
    )

    data class AppCacheInfo(
        val packageName: String,
        val appName: String,
        val cacheBytes: Long,
        val isSystemApp: Boolean
    )

    fun hasUsageAccess(ctx: Context): Boolean {
        return try {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            } else {
                @Suppress("DEPRECATION") appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) { false }
    }

    fun openUsageAccessSettings(ctx: Context) {
        PermissionHelper.openPermissionSettings(ctx, PermissionHelper.PermissionType.USAGE_ACCESS)
    }

    fun openAppInfoSettings(ctx: Context, packageName: String) {
        try {
            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) { 
            Log.w(TAG, "openAppInfoSettings gagal, fallback ke Info App kita sendiri: ${e.message}")
            PermissionHelper.openPermissionSettings(ctx, PermissionHelper.PermissionType.STORAGE)
        }
    }

    fun getAllAppsCache(ctx: Context): List<AppCacheInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()
        if (!hasUsageAccess(ctx)) return emptyList()

        val result = mutableListOf<AppCacheInfo>()
        try {
            val pm = ctx.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getInstalledPackages(0)
            }
            val sm = ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager

            packages.forEach { pkg ->
                if (pkg.packageName == ctx.packageName) return@forEach
                val appInfo: ApplicationInfo = pkg.applicationInfo ?: return@forEach
                try {
                    val stats = sm.queryStatsForPackage(StorageManager.UUID_DEFAULT, pkg.packageName, Process.myUserHandle())
                    if (stats.cacheBytes >= 1_000_000L) {
                        result.add(AppCacheInfo(
                            packageName = pkg.packageName,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            cacheBytes = stats.cacheBytes,
                            isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        ))
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) { Log.w(TAG, "getAllAppsCache: ${e.message}") }
        return result.sortedByDescending { it.cacheBytes }.take(30)
    }

    fun getOwnCacheSize(ctx: Context): Long {
        return try {
            var total = 0L
            ctx.cacheDir?.let { total += dirSize(it) }
            ctx.externalCacheDir?.let { total += dirSize(it) }
            ctx.codeCacheDir?.let { total += dirSize(it) }
            total
        } catch (e: Exception) { 0L }
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        try { dir.walkTopDown().forEach { if (it.isFile) total += it.length() } } catch (_: Exception) {}
        return total
    }

    private fun clearDir(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        try {
            dir.listFiles()?.forEach { file ->
                freed += if (file.isDirectory) {
                    val sub = clearDir(file)
                    if (file.delete()) sub + file.length() else sub
                } else {
                    val size = file.length()
                    if (file.delete()) size else 0L
                }
            }
        } catch (_: Exception) {}
        return freed
    }

    fun clearOwnCache(ctx: Context): Long {
        var freed = 0L
        try {
            freed += clearDir(ctx.cacheDir)
            freed += clearDir(ctx.externalCacheDir)
            freed += clearDir(ctx.codeCacheDir)
        } catch (e: Exception) { Log.w(TAG, "clearOwnCache: ${e.message}") }
        return freed
    }

    fun killBackgroundApps(ctx: Context): Int {
        var count = 0
        try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pm = ctx.packageManager
            val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getInstalledPackages(0)
            }
            packages.forEach { pkg ->
                if (pkg.packageName != ctx.packageName) {
                    try { am.killBackgroundProcesses(pkg.packageName); count++ } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        return count
    }

    fun getFreeRamMb(ctx: Context): Long {
        return try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            mi.availMem / (1024 * 1024)
        } catch (_: Exception) { 0L }
    }

    fun getTotalRamMb(ctx: Context): Long {
        return try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            mi.totalMem / (1024 * 1024)
        } catch (_: Exception) { 0L }
    }

    fun getFreeStorageMb(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) stat.availableBytes else stat.availableBlocks.toLong() * stat.blockSize.toLong()
            freeBytes / (1024 * 1024)
        } catch (_: Exception) { 0L }
    }

    fun boostAll(ctx: Context): BoostResult {
        return try {
            val ramBefore = getFreeRamMb(ctx)
            val cacheFreed = clearOwnCache(ctx)
            val appsKilled = killBackgroundApps(ctx)
            System.gc()
            Thread.sleep(400)
            val ramAfter = getFreeRamMb(ctx)
            val ramFreed = (ramAfter - ramBefore).coerceAtLeast(0)

            val msg = buildString {
                append("Cache dibersihkan: ${formatBytes(cacheFreed)}")
                if (appsKilled > 0) append(" · $appsKilled app di-kill")
                if (ramFreed > 0) append(" · RAM +${ramFreed} MB")
            }
            BoostResult(cacheFreed, ramBefore, ramAfter, ramFreed, appsKilled, true, msg)
        } catch (e: Exception) {
            BoostResult(0L, 0L, 0L, 0L, 0, false, "Gagal: ${e.message}")
        }
    }

    fun openStorageSettings(ctx: Context) {
        try {
            val intents = listOf(
                Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
                Intent(Settings.ACTION_SETTINGS),
                Intent("android.settings.STORAGE_SETTINGS")
            )
            for (intent in intents) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    if (intent.resolveActivity(ctx.packageManager) != null) {
                        ctx.startActivity(intent)
                        return
                    }
                } catch (_: Exception) {}
            }
            // Fallback terakhir pakai Helper
            PermissionHelper.openPermissionSettings(ctx, PermissionHelper.PermissionType.STORAGE)
        } catch (_: Exception) {}
    }

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024L * 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

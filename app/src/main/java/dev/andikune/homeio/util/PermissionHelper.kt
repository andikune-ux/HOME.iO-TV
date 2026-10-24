package dev.andikune.homeio.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    enum class PermissionType {
        STORAGE, NOTIFICATION, INSTALL_APK, USAGE_ACCESS, NOTIF_LISTENER, WRITE_SETTINGS
    }

    fun checkPermission(context: Context, type: PermissionType): Boolean {
        return when (type) {
            PermissionType.STORAGE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            PermissionType.NOTIFICATION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
            PermissionType.INSTALL_APK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else true
            PermissionType.USAGE_ACCESS -> {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                } else {
                    @Suppress("DEPRECATION") appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
                }
                mode == AppOpsManager.MODE_ALLOWED
            }
            PermissionType.NOTIF_LISTENER -> NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
            PermissionType.WRITE_SETTINGS -> Settings.System.canWrite(context)
        }
    }

    // FIX: Mengembalikan true jika halaman pengaturan berhasil dibuka (termasuk Info App)
    // Mengembalikan false HANYA jika TV benar-benar tidak punya halaman pengaturan tersebut (System Limit)
    fun openPermissionSettings(context: Context, type: PermissionType): Boolean {
        val intent = when (type) {
            PermissionType.STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply { data = Uri.parse("package:${context.packageName}") }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }
                }
            }
            PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }
                }
            }
            PermissionType.INSTALL_APK -> Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = Uri.parse("package:${context.packageName}") }
            PermissionType.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            PermissionType.NOTIF_LISTENER -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            PermissionType.WRITE_SETTINGS -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply { data = Uri.parse("package:${context.packageName}") }
        }

        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback ke Info Aplikasi: User masih bisa atur manual di sini, jadi return true
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                Toast.makeText(context, "Halaman izin khusus tidak tersedia. Silakan atur manual di Info Aplikasi.", Toast.LENGTH_LONG).show()
                true 
            } catch (_: Exception) {
                // Benar-benar tidak ada halaman pengaturan (System Limit)
                Toast.makeText(context, "Sistem TV tidak mendukung pengaturan izin.", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
}

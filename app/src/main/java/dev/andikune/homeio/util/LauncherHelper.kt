package dev.andikune.homeio.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.role.RoleManager
import android.os.Build
import android.provider.Settings

object LauncherHelper {

    /**
     * Cek apakah Home.io sudah menjadi Default Launcher (Home).
     */
    fun isDefaultLauncher(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }

        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == context.packageName
    }

    /**
     * Membuat intent resmi untuk meminta aplikasi ini menjadi Home role.
     * Android 10+ harus memakai RoleManager; selector ACTION_HOME hanya
     * menjalankan launcher yang sudah dipilih dan tidak mengubah default.
     */
    fun createDefaultLauncherRequestIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }

        return Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun openDefaultLauncherRequest(context: Context): Boolean {
        return try {
            context.startActivity(
                createDefaultLauncherRequestIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openHomeSettings(context: Context): Boolean {
        return try {
            context.startActivity(
                Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Paksa sistem menampilkan dialog pemilihan Home Launcher.
     */
    fun openLauncherChooser(context: Context) {
        openDefaultLauncherRequest(context)
    }
}

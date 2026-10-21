package dev.andikune.homeio.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

object LauncherHelper {

    /**
     * Cek apakah Home.io sudah menjadi Default Launcher (Home).
     */
    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val res = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return res?.activityInfo?.packageName == context.packageName
    }

    /**
     * Paksa sistem menampilkan dialog pemilihan Home Launcher.
     */
    fun openLauncherChooser(context: Context) {
        try {
            // Trik memaksa dialog pemilihan launcher muncul
            val selector = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_HOME)
            selector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(selector)
        } catch (e: Exception) {
            // Fallback jika TV tidak mendukung trik di atas
            try {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e2: Exception) {
                // Abaikan jika gagal total
            }
        }
    }
}

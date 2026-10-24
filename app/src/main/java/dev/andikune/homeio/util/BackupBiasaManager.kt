package dev.andikune.homeio.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import dev.andikune.homeio.domain.model.SettingsConfig
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupBiasaManager {
    private const val TAG = "BackupBiasa"
    private const val FOLDER = "HomeiO/Backup"
    private val jsonFormat = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true 
    }

    data class BackupResult(val success: Boolean, val path: String? = null, val msg: String? = null)

    // FIX: Fungsi untuk mengecek fallback, agar UI bisa nampilin peringatan
    fun isUsingFallbackPath(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            val dir = File(Environment.getExternalStorageDirectory(), FOLDER)
            !dir.exists() || !dir.canWrite()
        }
    }

    private fun getDir(ctx: Context): File {
        // PLAN A: Paksa simpan di root SD Card (Butuh Izin)
        if (!isUsingFallbackPath(ctx)) {
            val dir = File(Environment.getExternalStorageDirectory(), FOLDER)
            if (!dir.exists()) dir.mkdirs()
            if (dir.exists() && dir.canWrite()) return dir
        }
        
        // PLAN B: Fallback ke folder Downloads bawaan TV
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fallbackDir = File(downloadsDir, FOLDER)
        if (!fallbackDir.exists()) fallbackDir.mkdirs()
        if (fallbackDir.exists() && fallbackDir.canWrite()) return fallbackDir

        // Fallback Terakhir: Folder privat aplikasi (pasti aman)
        return File(ctx.filesDir, FOLDER).also { if (!it.exists()) it.mkdirs() }
    }

    fun createBackup(ctx: Context, config: SettingsConfig): BackupResult {
        return try {
            val jsonString = jsonFormat.encodeToString(SettingsConfig.serializer(), config)
            val timeStamp = SimpleDateFormat("HHmmss_ddMMyyyy", Locale.getDefault()).format(Date())
            val fileName = "Backup_HomeIO_$timeStamp.json"
            val file = File(getDir(ctx), fileName)
            file.writeText(jsonString, Charsets.UTF_8)
            BackupResult(true, file.absolutePath, "Backup berhasil")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal backup: ${e.message}")
            BackupResult(false, msg = e.message)
        }
    }

    fun restoreBackup(filePath: String): SettingsConfig? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null
            val jsonString = file.readText(Charsets.UTF_8)
            jsonFormat.decodeFromString(SettingsConfig.serializer(), jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal restore: ${e.message}")
            null
        }
    }

    fun getBackupList(ctx: Context): List<File> =
        getDir(ctx).listFiles()?.filter { it.name.endsWith(".json") }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun deleteBackup(filePath: String): Boolean = try { File(filePath).delete() } catch (e: Exception) { false }
}

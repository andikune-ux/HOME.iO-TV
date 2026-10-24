package dev.andikune.homeio.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import dev.andikune.homeio.BuildConfig
import dev.andikune.homeio.data.AppKnowledge
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {
    private const val TAG = "BackupManager"
    private const val FOLDER_APP = "HomeiO"
    private const val FOLDER_BACKUP = "Backup aman"

    data class BackupResult(
        val success: Boolean,
        val filePath: String? = null,
        val errorMessage: String? = null,
        val isFallbackPath: Boolean = false
    )

    fun isUsingFallbackPath(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            false
        }
    }

    fun generateBackupContent(context: Context): String {
        val timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val version = try { BuildConfig.VERSION_NAME } catch (e: Exception) { "Unknown" }

        return buildString {
            appendLine("============================================================")
            appendLine("BACKUP AMAN - HOME.IO TV")
            appendLine("============================================================")
            appendLine("Tanggal Export  : $timestamp")
            appendLine("Versi Aplikasi  : $version")
            appendLine("Device          : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android Version : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("============================================================")
            appendLine()
            appendLine("=== STRUKTUR APLIKASI ===")
            appendLine(AppKnowledge.APP_STRUCTURE)
            appendLine()
            appendLine("=== FITUR APLIKASI ===")
            appendLine(AppKnowledge.APP_FEATURES)
            appendLine()
            appendLine("=== ATURAN KERJA ===")
            appendLine(AppKnowledge.WORKFLOW_RULES)
            appendLine()
            appendLine("=== RIWAYAT MASALAH & PERBAIKAN ===")
            appendLine(AppKnowledge.KNOWN_ISSUES)
            appendLine()
            appendLine("=== RIWAYAT BUILD ERROR ===")
            appendLine(AppKnowledge.BUILD_ERROR_HISTORY)
            appendLine()
            appendLine("=== INFORMASI BACKUP ===")
            appendLine(AppKnowledge.BACKUP_INFO)
            appendLine()
            appendLine("=== SOSIAL MEDIA ===")
            appendLine("- WhatsApp  : ${AppKnowledge.SOCIAL_WA}")
            appendLine("- TikTok    : ${AppKnowledge.SOCIAL_TIKTOK}")
            appendLine("- Instagram : ${AppKnowledge.SOCIAL_IG}")
            appendLine()
            appendLine("=== AI PROMPT ===")
            appendLine(AppKnowledge.AI_PROMPT)
        }
    }

    fun saveBackupToFile(context: Context, content: String): BackupResult {
        val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val fileName = "Backup Aman-Home.io-$dateString.txt"

        // PLAN A: Paksa simpan di root SD Card (Butuh Izin)
        if (!isUsingFallbackPath()) {
            try {
                val backupDir = File(Environment.getExternalStorageDirectory(), "$FOLDER_APP/$FOLDER_BACKUP")
                if (!backupDir.exists()) backupDir.mkdirs()
                if (backupDir.exists() && backupDir.canWrite()) {
                    val file = File(backupDir, fileName)
                    file.writeText(content, Charsets.UTF_8)
                    return BackupResult(success = true, filePath = file.absolutePath, isFallbackPath = false)
                }
            } catch (e: Exception) { Log.e(TAG, "Plan A gagal: ${e.message}") }
        }

        // PLAN B: Fallback ke folder Downloads bawaan TV
        return saveFallback(context, fileName, content)
    }

    private fun saveFallback(context: Context, fileName: String, content: String): BackupResult {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fallbackDir = File(downloadsDir, "$FOLDER_APP/$FOLDER_BACKUP")
            
            if (!fallbackDir.exists()) fallbackDir.mkdirs()

            if (fallbackDir.exists() && fallbackDir.canWrite()) {
                val file = File(fallbackDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                BackupResult(success = true, filePath = file.absolutePath, isFallbackPath = true)
            } else {
                // Jika folder publik benar-benar gagal, simpan di folder privat (pasti aman)
                val appDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                val internalDir = File(appDir, "$FOLDER_APP/$FOLDER_BACKUP")
                if (!internalDir.exists()) internalDir.mkdirs()
                val file = File(internalDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                BackupResult(success = true, filePath = file.absolutePath, isFallbackPath = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Plan B gagal: ${e.message}", e)
            BackupResult(success = false, errorMessage = e.message, isFallbackPath = true)
        }
    }
}

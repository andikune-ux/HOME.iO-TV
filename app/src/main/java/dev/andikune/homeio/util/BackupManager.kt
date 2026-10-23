package dev.andikune.homeio.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import dev.andikune.homeio.BuildConfig
import dev.andikune.homeio.data.AppKnowledge
import dev.andikune.homeio.domain.model.SettingsConfig
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

    fun needsStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else {
            false
        }
    }

    fun openPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Gagal buka permission settings: ${e2.message}")
                }
            }
        }
    }

    fun isUsingFallbackPath(): Boolean {
        return try {
            val baseDir = Environment.getExternalStorageDirectory()
            val backupDir = File(baseDir, "$FOLDER_APP/$FOLDER_BACKUP")
            !backupDir.exists() || !backupDir.canWrite()
        } catch (e: Exception) {
            true
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
        if (needsStoragePermission()) {
            return BackupResult(
                success = false,
                errorMessage = "Izin akses penyimpanan diperlukan",
                isFallbackPath = true
            )
        }

        return try {
            val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val fileName = "Backup Aman-Home.io-$dateString.txt"
            
            val baseDir = Environment.getExternalStorageDirectory()
            val backupDir = File(baseDir, "$FOLDER_APP/$FOLDER_BACKUP")
            
            if (!backupDir.exists()) backupDir.mkdirs()
            
            if (backupDir.exists() && backupDir.canWrite()) {
                val file = File(backupDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                return BackupResult(success = true, filePath = file.absolutePath, isFallbackPath = false)
            } else {
                saveFallback(context, fileName, content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup gagal: ${e.message}", e)
            val dateString = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            saveFallback(context, "Backup Aman-Home.io-$dateString.txt", content)
        }
    }

    private fun saveFallback(context: Context, fileName: String, content: String): BackupResult {
        return try {
            val appDir = File(context.filesDir, "$FOLDER_APP/$FOLDER_BACKUP")
            if (!appDir.exists()) appDir.mkdirs()
            val file = File(appDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            BackupResult(success = true, filePath = file.absolutePath, isFallbackPath = true)
        } catch (e: Exception) {
            Log.e(TAG, "Fallback gagal: ${e.message}", e)
            BackupResult(success = false, errorMessage = e.message, isFallbackPath = true)
        }
    }
}

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
        val needPermission: Boolean = false,
        val isFallbackPath: Boolean = false
    )

    fun needsStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            !Environment.isExternalStorageManager()
        } else false
    }

    fun isUsingFallbackPath(): Boolean {
        val external = File(Environment.getExternalStorageDirectory(), "$FOLDER_APP/$FOLDER_BACKUP")
        return !external.exists() || !external.canWrite()
    }

    fun openPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {}
            }
        }
    }

    fun generateBackupContent(context: Context): String {
        val timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val version = try { BuildConfig.VERSION_NAME } catch (_: Exception) { "Unknown" }
        val versionCode = try { BuildConfig.VERSION_CODE } catch (_: Exception) { 0 }

        return buildString {
            appendLine("=".repeat(60))
            appendLine("BACKUP AMAN - HOME.IO")
            appendLine("=".repeat(60))
            appendLine("Tanggal Export  : $timestamp")
            appendLine("Versi Aplikasi  : $version (build $versionCode)")
            appendLine("Device          : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android Version : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Package         : ${context.packageName}")
            appendLine("=".repeat(60))
            appendLine()

            appendLine("=".repeat(60))
            appendLine("PENGATURAN USER SAAT INI")
            appendLine("=".repeat(60))
            appendLine(dumpAllPrefs(context))
            appendLine()

            appendLine("=".repeat(60))
            appendLine("MEMORY KNOWLEDGE & INSTRUCTION")
            appendLine("=".repeat(60))
            appendLine("Nama Aplikasi  : ${AppKnowledge.APP_NAME}")
            appendLine("Deskripsi      : ${AppKnowledge.APP_DESCRIPTION}")
            appendLine("Repo           : ${AppKnowledge.REPO_URL}")
            appendLine()
            appendLine(AppKnowledge.APP_STRUCTURE)
            appendLine()
            appendLine(AppKnowledge.APP_FEATURES)
            appendLine()
            appendLine(AppKnowledge.WORKFLOW_RULES)
            appendLine()
            appendLine(AppKnowledge.DEVELOPER_INSTRUCTION)
            appendLine()
            appendLine(AppKnowledge.KNOWN_ISSUES)
            appendLine()
            appendLine(AppKnowledge.BUILD_ERROR_HISTORY)
            appendLine()
            appendLine(AppKnowledge.BACKUP_INFO)
            appendLine()
            appendLine("=== MEDIA SOSIAL ===")
            appendLine("WhatsApp : ${AppKnowledge.SOCIAL_WA}")
            appendLine("TikTok   : ${AppKnowledge.SOCIAL_TIKTOK}")
            appendLine("Instagram: ${AppKnowledge.SOCIAL_IG}")
            appendLine()
            appendLine("=== AI PROMPT (SALIN UNTUK AI BARU) ===")
            appendLine(AppKnowledge.AI_PROMPT)
            appendLine()

            appendLine("=".repeat(60))
            appendLine("END OF BACKUP")
            appendLine("=".repeat(60))
        }
    }

    private fun dumpAllPrefs(context: Context): String {
        return try {
            val dataDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (!dataDir.exists() || !dataDir.isDirectory) return "(Tidak ada SharedPreferences)"
            val files = dataDir.listFiles()?.filter { it.extension == "xml" }.orEmpty()
            if (files.isEmpty()) return "(kosong)"

            val sb = StringBuilder()
            files.forEach { file ->
                sb.appendLine("--- ${file.nameWithoutExtension} ---")
                sb.appendLine(file.readText().take(3000))
                sb.appendLine()
            }
            sb.toString().trimEnd()
        } catch (e: Exception) {
            "(Gagal baca prefs: ${e.message})"
        }
    }

    fun saveBackupToFile(context: Context, content: String): BackupResult {
        if (needsStoragePermission()) {
            return BackupResult(
                success = false,
                errorMessage = "Izin akses penyimpanan diperlukan",
                needPermission = true
            )
        }

        return try {
            val dateTime = SimpleDateFormat("dd-MM-yyyy-HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Backup Aman-Home.io-$dateTime.txt"

            val backupDir = File(Environment.getExternalStorageDirectory(), "$FOLDER_APP/$FOLDER_BACKUP")
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                return saveFallback(context, content, fileName)
            }

            val file = File(backupDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            Log.d(TAG, "Backup tersimpan: ${file.absolutePath}")
            BackupResult(success = true, filePath = file.absolutePath, isFallbackPath = false)
        } catch (e: Exception) {
            Log.e(TAG, "Backup gagal: ${e.message}", e)
            saveFallback(context, content, "Backup Aman-Home.io-fallback.txt")
        }
    }

    private fun saveFallback(context: Context, content: String, fileName: String): BackupResult {
        return try {
            val appDir = File(context.filesDir, "$FOLDER_APP/$FOLDER_BACKUP")
            if (!appDir.exists()) appDir.mkdirs()
            val file = File(appDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            Log.d(TAG, "Backup (fallback) tersimpan: ${file.absolutePath}")
            BackupResult(success = true, filePath = file.absolutePath, isFallbackPath = true)
        } catch (e: Exception) {
            BackupResult(success = false, errorMessage = e.message ?: "Gagal menyimpan file")
        }
    }
}

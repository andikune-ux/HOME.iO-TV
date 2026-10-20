package dev.andikune.homeio.data.local

import android.os.Environment
import dev.andikune.homeio.domain.model.SettingsConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private fun getBackupDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "HomeiO/Backup")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun listBackupFiles(): List<File> {
        val dir = getBackupDir()
        return dir.listFiles { file -> file.extension.equals("json", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun createBackup(config: SettingsConfig): File {
        val timeStamp = SimpleDateFormat("HHmmss_ddMMyyyy", Locale.US).format(Date())
        val fileName = "Backup_HomeIO_$timeStamp.json"
        val file = File(getBackupDir(), fileName)
        val jsonString = json.encodeToString(config)
        file.writeText(jsonString)
        return file
    }

    fun restoreBackup(file: File): SettingsConfig {
        val content = file.readText()
        return json.decodeFromString(content)
    }

    fun deleteBackup(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }
}

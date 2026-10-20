package dev.andikune.homeio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import dev.andikune.homeio.util.ApkDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * UpdateDownloadService — Foreground Service untuk download APK OTA.
 *
 * Fungsi:
 *   - Persistent Notification di status bar (tidak batal meskipun user tekan Back)
 *   - Download tetap jalan meskipun aplikasi di-background
 *   - State disimpan di SharedPreferences (bisa dibaca UI lewat readState)
 *   - Broadcast ACTION_PROGRESS untuk update progress bar real-time
 *
 * WAJIB: Manifest harus deklarasikan service ini dengan
 *   android:foregroundServiceType="dataSync"
 * dan permission FOREGROUND_SERVICE_DATA_SYNC.
 */
class UpdateDownloadService : Service() {

    companion object {
        private const val TAG = "UpdateDownloadService"
        private const val CHANNEL_ID = "homeio_update_download"
        private const val NOTIFICATION_ID = 8890

        const val ACTION_START = "dev.andikune.homeio.START_UPDATE_DOWNLOAD"
        const val ACTION_PROGRESS = "dev.andikune.homeio.UPDATE_DOWNLOAD_PROGRESS"

        const val EXTRA_URL = "download_url"
        const val EXTRA_FILE_NAME = "file_name"

        const val PREFS = "update_download_state"
        const val KEY_STATUS = "status"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"
        const val KEY_ERROR = "error"

        const val STATUS_IDLE = "IDLE"
        const val STATUS_DOWNLOADING = "DOWNLOADING"
        const val STATUS_COMPLETE = "COMPLETE"
        const val STATUS_ERROR = "ERROR"

        /**
         * Mulai download dari luar service (dari Activity).
         * Otomatis pakai Foreground Service di Android 8+.
         */
        fun start(context: Context, downloadUrl: String, fileName: String) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, downloadUrl)
                putExtra(EXTRA_FILE_NAME, fileName)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal start service: ${e.message}", e)
                writeState(context, STATUS_ERROR, error = e.message ?: "Service gagal start")
            }
        }

        /**
         * Baca state terakhir (dipakai UI untuk tahu progress & path file).
         */
        fun readState(context: Context): State {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return State(
                status = prefs.getString(KEY_STATUS, STATUS_IDLE) ?: STATUS_IDLE,
                progress = prefs.getFloat(KEY_PROGRESS, 0f),
                filePath = prefs.getString(KEY_FILE_PATH, null),
                error = prefs.getString(KEY_ERROR, null)
            )
        }

        private fun writeState(
            context: Context,
            status: String,
            progress: Float = 0f,
            filePath: String? = null,
            error: String? = null
        ) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_STATUS, status)
                .putFloat(KEY_PROGRESS, progress.coerceIn(0f, 1f))
                .putString(KEY_FILE_PATH, filePath)
                .putString(KEY_ERROR, error)
                .commit()
        }

        data class State(
            val status: String,
            val progress: Float,
            val filePath: String?,
            val error: String?
        )
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) {
            // Kalau service di-restart tanpa action, langsung stop
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Menyiapkan download update..."))

        if (activeJob) {
            Log.d(TAG, "Download sudah aktif, abaikan request baru")
            return START_REDELIVER_INTENT
        }

        val url = intent.getStringExtra(EXTRA_URL)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME)

        if (url.isNullOrBlank() || fileName.isNullOrBlank()) {
            writeState(this, STATUS_ERROR, error = "URL atau nama file kosong")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        activeJob = true
        writeState(this, STATUS_DOWNLOADING)
        broadcastState()

        serviceScope.launch {
            try {
                ApkDownloader.downloadApk(applicationContext, url, fileName).collect { state ->
                    when {
                        // ==== ERROR ====
                        state.errorMessage != null -> {
                            writeState(
                                this@UpdateDownloadService,
                                STATUS_ERROR,
                                error = state.errorMessage
                            )
                            updateNotification("Download gagal")
                            broadcastState()
                        }

                        // ==== COMPLETE ====
                        state.isFinished && state.savedFilePath != null -> {
                            val apkFile = java.io.File(state.savedFilePath)
                            if (ApkDownloader.isValidApk(applicationContext, apkFile)) {
                                writeState(
                                    this@UpdateDownloadService,
                                    STATUS_COMPLETE,
                                    progress = 1f,
                                    filePath = state.savedFilePath
                                )
                                updateNotification("Download update selesai")
                                Log.d(TAG, "Download complete: ${state.savedFilePath}")
                            } else {
                                apkFile.delete()
                                writeState(
                                    this@UpdateDownloadService,
                                    STATUS_ERROR,
                                    error = "APK tidak valid (corrupt atau bukan file Home.io)"
                                )
                                updateNotification("Download gagal: APK tidak valid")
                            }
                            broadcastState()
                        }

                        // ==== DOWNLOADING ====
                        else -> {
                            writeState(
                                this@UpdateDownloadService,
                                STATUS_DOWNLOADING,
                                progress = state.progress
                            )
                            updateNotification("Mengunduh update... ${(state.progress * 100).toInt()}%")
                            broadcastState()
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download service error: ${e.message}", e)
                writeState(
                    this@UpdateDownloadService,
                    STATUS_ERROR,
                    error = e.message ?: "Gagal download"
                )
                broadcastState()
            } finally {
                activeJob = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_REDELIVER_INTENT
    }

    private fun broadcastState() {
        sendBroadcast(Intent(ACTION_PROGRESS).setPackage(packageName))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Update Home.io",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi progres download update aplikasi"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Home.io — Update")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(message: String) {
        try {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(message))
        } catch (e: Exception) {
            Log.w(TAG, "Gagal update notification: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

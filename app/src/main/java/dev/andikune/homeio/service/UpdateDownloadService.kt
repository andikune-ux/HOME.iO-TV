package dev.andikune.homeio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import dev.andikune.homeio.MainActivity
import dev.andikune.homeio.util.ApkDownloader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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

        fun start(context: Context, downloadUrl: String, fileName: String) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                action = ACTION_START; putExtra(EXTRA_URL, downloadUrl); putExtra(EXTRA_FILE_NAME, fileName)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(context, intent) else context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "start service error: ${e.message}", e)
                writeState(context, STATUS_ERROR, error = e.message ?: "Service gagal")
            }
        }

        fun readState(context: Context): State {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return State(
                status = prefs.getString(KEY_STATUS, STATUS_IDLE) ?: STATUS_IDLE,
                progress = prefs.getFloat(KEY_PROGRESS, 0f),
                filePath = prefs.getString(KEY_FILE_PATH, null),
                error = prefs.getString(KEY_ERROR, null)
            )
        }

        fun getCompletedFilePath(context: Context): String? {
            val state = readState(context)
            return if (state.status == STATUS_COMPLETE) state.filePath else null
        }

        fun resetState(context: Context) = writeState(context, STATUS_IDLE)

        private fun writeState(context: Context, status: String, progress: Float = 0f, filePath: String? = null, error: String? = null) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_STATUS, status).putFloat(KEY_PROGRESS, progress.coerceIn(0f, 1f))
                .putString(KEY_FILE_PATH, filePath).putString(KEY_ERROR, error).commit()
        }

        data class State(val status: String, val progress: Float, val filePath: String?, val error: String?)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob = false

    override fun onCreate() { super.onCreate(); createNotificationChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START) { stopSelf(startId); return START_NOT_STICKY }
        try {
            val notification = buildNotification("Menyiapkan download...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) 
            else startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}", e); writeState(this, STATUS_ERROR, error = "Gagal start foreground service"); stopSelf(startId); return START_NOT_STICKY
        }

        if (activeJob) return START_REDELIVER_INTENT
        val url = intent.getStringExtra(EXTRA_URL); val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
        if (url.isNullOrBlank() || fileName.isNullOrBlank()) { writeState(this, STATUS_ERROR, error = "URL atau nama file kosong"); stopSelf(startId); return START_NOT_STICKY }

        activeJob = true; writeState(this, STATUS_DOWNLOADING); broadcastState()

        serviceScope.launch {
            try {
                ApkDownloader.download(applicationContext, url, fileName).collect { state ->
                    when {
                        state.error != null -> { writeState(this@UpdateDownloadService, STATUS_ERROR, error = state.error); updateNotification("Download gagal"); broadcastState() }
                        state.isFinished && state.savedFilePath != null -> {
                            val apkFile = java.io.File(state.savedFilePath)
                            if (ApkDownloader.isValid(applicationContext, apkFile)) { writeState(this@UpdateDownloadService, STATUS_COMPLETE, progress = 1f, filePath = state.savedFilePath); updateNotification("Download selesai — Ketuk untuk Install") } 
                            else { apkFile.delete(); writeState(this@UpdateDownloadService, STATUS_ERROR, error = "APK tidak valid"); updateNotification("Download gagal: APK tidak valid") }
                            broadcastState()
                        }
                        else -> { writeState(this@UpdateDownloadService, STATUS_DOWNLOADING, progress = state.progress); updateNotification("Mengunduh... ${(state.progress * 100).toInt()}%"); broadcastState() }
                    }
                }
            } catch (e: CancellationException) { throw e } 
            catch (e: Exception) { writeState(this@UpdateDownloadService, STATUS_ERROR, error = e.message ?: "Gagal"); broadcastState() } 
            finally { activeJob = false; stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(startId) }
        }
        return START_REDELIVER_INTENT
    }

    private fun broadcastState() = sendBroadcast(Intent(ACTION_PROGRESS).setPackage(packageName))

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Download Update Home.io", NotificationManager.IMPORTANCE_LOW).apply { description = "Progres download update aplikasi"; setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(msg: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingFlags)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID) else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder.setContentTitle("Home.io — Update").setContentText(msg).setSmallIcon(android.R.drawable.stat_sys_download).setOngoing(true).setPriority(Notification.PRIORITY_LOW).setContentIntent(pendingIntent).build()
    }

    private fun updateNotification(msg: String) {
        try { (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(NOTIFICATION_ID, buildNotification(msg)) } catch (e: Exception) { Log.w(TAG, "updateNotif error") }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { serviceScope.cancel(); super.onDestroy() }
}

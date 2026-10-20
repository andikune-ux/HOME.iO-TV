package dev.andikune.homeio.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import dev.andikune.homeio.util.ApkDownloader
import kotlinx.coroutines.*

class UpdateDownloadService : Service() {
    companion object {
        private const val CH_ID = "homeio_update"
        private const val NOTIF_ID = 8890
        const val ACTION_START = "dev.andikune.homeio.START_UPDATE"
        const val ACTION_PROGRESS = "dev.andikune.homeio.UPDATE_PROGRESS"
        
        const val PREFS = "update_state"
        const val KEY_STATUS = "status"
        const val KEY_PROGRESS = "progress"
        const val KEY_PATH = "path"
        const val KEY_ERROR = "error"

        fun start(ctx: Context, url: String, name: String) {
            val i = Intent(ctx, UpdateDownloadService::class.java).apply {
                action = ACTION_START; putExtra("url", url); putExtra("name", name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(ctx, i) else ctx.startService(i)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CH_ID, "Update", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != ACTION_START || isRunning) return START_NOT_STICKY
        startForeground(NOTIF_ID, buildNotif("Menyiapkan download..."))
        
        val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
        val name = intent.getStringExtra("name") ?: return START_NOT_STICKY
        
        isRunning = true
        scope.launch {
            ApkDownloader.download(applicationContext, url, name).collect { state ->
                when {
                    state.error != null -> {
                        saveState("ERROR", 0f, null, state.error); broadcast(); stopSelf()
                    }
                    state.isFinished && state.savedFilePath != null -> {
                        saveState("COMPLETE", 1f, state.savedFilePath, null)
                        updateNotif("Download selesai"); broadcast(); stopSelf()
                    }
                    else -> {
                        saveState("DOWNLOADING", state.progress, null, null)
                        updateNotif("Mengunduh... ${(state.progress * 100).toInt()}%"); broadcast()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun saveState(status: String, prog: Float, path: String?, err: String?) {
        getSharedPreferences(PREFS, 0).edit()
            .putString(KEY_STATUS, status).putFloat(KEY_PROGRESS, prog)
            .putString(KEY_PATH, path).putString(KEY_ERROR, err).commit()
    }

    private fun broadcast() = sendBroadcast(Intent(ACTION_PROGRESS).setPackage(packageName))

    private fun buildNotif(msg: String): Notification {
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CH_ID) else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setContentTitle("Home.io Update").setContentText(msg).setSmallIcon(android.R.drawable.stat_sys_download).setOngoing(true).build()
    }

    private fun updateNotif(msg: String) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotif(msg))
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

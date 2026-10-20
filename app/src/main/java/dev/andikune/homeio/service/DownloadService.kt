package dev.andikune.homeio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.andikune.homeio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DownloadService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val channelId = "homeio_download_channel"
    private val notificationId = 1001

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadUrl = intent?.getStringExtra(EXTRA_URL)
        val fileName = intent?.getStringExtra(EXTRA_FILENAME)

        val notification = createNotification("Downloading update...", 0)
        startForeground(notificationId, notification)

        if (!downloadUrl.isNullOrEmpty()) {
            serviceScope.launch {
                try {
                    downloadFile(downloadUrl, fileName)
                } catch (e: Exception) {
                    notifyDownloadFailed(e.message)
                } finally {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun downloadFile(urlStr: String, requestedFileName: String?) {
        val targetDir = File(
            android.os.Environment.getExternalStorageDirectory(),
            "HomeiO/Pembaharuan"
        )
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("HHmmss_ddMMyyyy", Locale.US).format(Date())
        val finalFileName = requestedFileName ?: "HomeIO_Update_$timeStamp.apk"
        val outputFile = File(targetDir, finalFileName)

        val url = URL(urlStr)
        val connection = url.openConnection()
        connection.connect()
        val fileLength = connection.contentLength

        connection.getInputStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    output.write(data, 0, count)
                    if (fileLength > 0) {
                        val progress = ((total * 100) / fileLength).toInt()
                        val updateNotification = createNotification("Downloading: $progress%", progress)
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(notificationId, updateNotification)
                    }
                }
            }
        }

        val finishNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Download Complete")
            .setContentText("Saved to ${outputFile.name}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId + 1, finishNotification)
    }

    private fun notifyDownloadFailed(error: String?) {
        val failNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Download Failed")
            .setContentText(error ?: "Unknown network error")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId + 2, failNotification)
    }

    private fun createNotification(message: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Home.io Updater")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Home.io Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_URL = "extra_download_url"
        const val EXTRA_FILENAME = "extra_filename"

        fun startDownload(context: Context, url: String, fileName: String? = null) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILENAME, fileName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

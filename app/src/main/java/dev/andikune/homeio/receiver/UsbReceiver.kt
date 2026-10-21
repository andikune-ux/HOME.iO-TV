package dev.andikune.homeio.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * UsbReceiver — Deteksi saat flashdisk / USB OTG dicolokkan.
 *
 * Fitur:
 *   - Terima broadcast ACTION_MEDIA_MOUNTED & ACTION_USB_DEVICE_ATTACHED
 *   - Tampilkan notifikasi "Flashdisk terdeteksi - buka File Manager"
 *   - Klik notifikasi → buka MainActivity dengan extra "OPEN_FILE_MANAGER"
 *   - HomeScreen akan tangkap extra ini dan langsung buka FileManagerDialog
 */
class UsbReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UsbReceiver"
        private const val CHANNEL_ID = "homeio_usb"
        private const val NOTIF_ID = 7777
        const val EXTRA_OPEN_FILE_MANAGER = "open_file_manager"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "USB event: $action")

        when (action) {
            Intent.ACTION_MEDIA_MOUNTED,
            Intent.ACTION_MEDIA_REMOVED,
            Intent.ACTION_MEDIA_UNMOUNTED,
            "android.hardware.usb.action.USB_DEVICE_ATTACHED",
            "android.hardware.usb.action.USB_DEVICE_DETACHED" -> {
                // Hanya tampilkan notifikasi saat DIPASANG, bukan dicabut
                if (action == Intent.ACTION_MEDIA_MOUNTED ||
                    action == "android.hardware.usb.action.USB_DEVICE_ATTACHED") {
                    showUsbNotification(context)
                }
            }
        }
    }

    private fun showUsbNotification(context: Context) {
        try {
            // Buat channel
            val channel = NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName("USB Flashdisk")
                .setDescription("Notifikasi saat flashdisk dicolokkan")
                .build()
            NotificationManagerCompat.from(context).createNotificationChannel(channel)

            // Intent buka MainActivity dengan extra
            val launchIntent = Intent(context, dev.andikune.homeio.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_FILE_MANAGER, true)
            }

            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, launchIntent, pendingFlags
            )

            val notif = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Flashdisk Terdeteksi")
                .setContentText("Tap untuk buka File Manager Home.io")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        } catch (e: Exception) {
            Log.e(TAG, "showUsbNotification: ${e.message}")
        }
    }
}

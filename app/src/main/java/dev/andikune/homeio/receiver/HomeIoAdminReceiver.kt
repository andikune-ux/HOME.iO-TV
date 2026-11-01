package dev.andikune.homeio.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class HomeIoAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "HOME.iO Admin Aktif. Fitur proteksi siap.", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "HOME.iO Admin Dimatikan.", Toast.LENGTH_SHORT).show()
    }
}

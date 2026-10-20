package dev.andikune.homeio.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import dev.andikune.homeio.MainActivity

class UsbReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "android.hardware.usb.action.USB_DEVICE_ATTACHED" ||
            action == Intent.ACTION_MEDIA_MOUNTED
        ) {
            Toast.makeText(context, "USB Storage Connected", Toast.LENGTH_SHORT).show()
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("NAVIGATE_TO", "FILE_MANAGER")
            }
            context.startActivity(launchIntent)
        }
    }
}

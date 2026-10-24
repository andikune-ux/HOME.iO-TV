package dev.andikune.homeio.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import dev.andikune.homeio.data.local.PreferencesManager
import dev.andikune.homeio.kiosk.AutoStartService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.QUICKBOOT_POWERON") return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val enabled = PreferencesManager(appContext).settingsFlow.first().isAutoStartOnBoot
                if (enabled) {
                    val serviceIntent = Intent(appContext, AutoStartService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(appContext, serviceIntent)
                    else appContext.startService(serviceIntent)
                }
            } catch (_: Exception) {
                // Boot receiver must never crash the system process.
            } finally {
                pendingResult.finish()
            }
        }
    }
}

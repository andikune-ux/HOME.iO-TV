package dev.andikune.homeio.kiosk

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import dev.andikune.homeio.MainActivity

class WatchdogService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            try {
                if (!KioskManager.isMainActivityForeground) {
                    Log.d("Watchdog", "MainActivity mati, membuka ulang...")
                    val intent = Intent(this@WatchdogService, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                }
            } catch (e: Exception) {}
            handler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        handler.postDelayed(checkRunnable, 30_000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() { handler.removeCallbacks(checkRunnable); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}

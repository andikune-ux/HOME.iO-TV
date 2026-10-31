package dev.andikune.homeio.presentation.screensaver

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class ScreensaverActivity : ComponentActivity() {
    
    // Variabel untuk menyimpan kecerahan asli TV
    private var originalBrightness: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Simpan kecerahan asli TV sebelum diubah
        try {
            originalBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            originalBrightness = 255 // Default maksimal jika gagal baca
        }

        // 2. Paksa kecerahan SISTEM ke 0 (Hanya jika diizinkan)
        if (Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 0)
            } catch (e: Exception) {
                Log.e("Screensaver", "Gagal set system brightness", e)
            }
        }

        // 3. Paksa kecerahan WINDOW ke 0
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.0f
        window.attributes = layoutParams
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 4. Sembunyikan system bar (status bar & navigation bar)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black) // Overlay Hitam Murni
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 5. KEMBALIKAN KECERAHAN ASLI saat screensaver ditutup
        if (originalBrightness != -1 && Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, originalBrightness)
            } catch (e: Exception) {
                Log.e("Screensaver", "Gagal restore brightness", e)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            finish() // Bangunkan layar saat remote ditekan
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        finish() // Bangunkan layar saat layar disentuh
        return true
    }
}

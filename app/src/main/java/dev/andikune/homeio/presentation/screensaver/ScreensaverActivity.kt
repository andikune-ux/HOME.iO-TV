package dev.andikune.homeio.presentation.screensaver

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
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
import androidx.compose.ui.graphics.Color as ComposeColor

class ScreensaverActivity : ComponentActivity() {
    
    private var originalBrightness: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Set Window Background jadi hitam pekat SEBELUM Compose render
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        // 2. Simpan kecerahan asli TV
        try {
            originalBrightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            originalBrightness = 255
        }

        // 3. Paksa kecerahan SISTEM ke 1 (kadang 0 diabaikan oleh OS TV)
        if (Settings.System.canWrite(this)) {
            try {
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 1)
            } catch (e: Exception) {
                Log.e("Screensaver", "Gagal set system brightness", e)
            }
        }

        // 4. Paksa kecerahan WINDOW ke 0.01f (trik agar OS TV mau nurunin)
        val layoutParams = window.attributes
        layoutParams.screenBrightness = 0.01f 
        window.attributes = layoutParams
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 5. Sembunyikan system bar
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
                    .background(ComposeColor.Black)
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 6. Kembalikan kecerahan asli
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
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        finish()
        return true
    }
}

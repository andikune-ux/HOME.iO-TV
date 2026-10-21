package dev.andikune.homeio.presentation.screensaver

import android.os.Build
import android.os.Bundle
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // FIX: Paksa layar menjadi 0% (Hitam Pekat)
        val layoutParams = window.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
        } else {
            layoutParams.screenBrightness = 0.0f
        }
        window.attributes = layoutParams
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // FIX: Sembunyikan system bar (status bar & navigation bar)
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
                    .background(Color.Black) // Warna Hitam Murni
            )
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

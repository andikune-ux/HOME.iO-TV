package dev.andikune.homeio.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.Log

object SoundFeedback {
    private var toneGenerator: ToneGenerator? = null
    private var lastPlayTime: Long = 0L
    private const val THROTTLE_MS = 80L

    fun playClick(context: Context) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlayTime < THROTTLE_MS) return
        lastPlayTime = now

        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 30)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
        } catch (e: Exception) {
            Log.e("SoundFeedback", "Error playing click: ${e.message}")
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {}
    }
}

package dev.andikune.homeio.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.util.Log

/**
 * SoundFeedback — Pemutar suara klik yang dioptimalkan.
 * FIX BUG #23 & #35:
 *   - Menggunakan Singleton ToneGenerator agar tidak bocor memori.
 *   - Throttle (jeda minimal 80ms) agar tidak hang saat D-Pad ditekan cepat.
 */
object SoundFeedback {
    private var toneGenerator: ToneGenerator? = null
    private var lastPlayTime: Long = 0L
    private const val THROTTLE_MS = 80L // Jeda minimal antar suara

    fun playClick(context: Context) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPlayTime < THROTTLE_MS) return // Abaikan jika terlalu cepat
        lastPlayTime = now

        try {
            if (toneGenerator == null) {
                // Gunakan volume 30% agar tidak terlalu nyaring (halus)
                toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 30)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
        } catch (e: Exception) {
            Log.e("SoundFeedback", "Error playing click: ${e.message}")
        }
    }

    /**
     * WAJIB dipanggil di onDestroy Activity agar tidak memory leak.
     */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            // Ignore
        }
    }
}

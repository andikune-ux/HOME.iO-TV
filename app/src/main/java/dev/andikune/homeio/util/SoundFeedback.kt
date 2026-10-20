package dev.andikune.homeio.util

import android.content.Context
import android.media.AudioManager
import android.view.SoundEffectConstants

object SoundFeedback {
    fun playClick(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        audioManager?.playSoundEffect(SoundEffectConstants.CLICK)
    }
}

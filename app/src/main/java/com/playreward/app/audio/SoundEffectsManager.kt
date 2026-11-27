package com.playreward.app.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundEffectsManager(private val context: Context) {
    @Volatile var soundEnabled: Boolean = true
    @Volatile var hapticsEnabled: Boolean = true

    private var toneGen: ToneGenerator? = null

    private fun getTone(): ToneGenerator? {
        if (toneGen == null) {
            toneGen = try {
                ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            } catch (_: Exception) {
                null
            }
        }
        return toneGen
    }

    fun playClick() {
        if (soundEnabled) {
            try { getTone()?.startTone(ToneGenerator.TONE_PROP_BEEP, 45) } catch (_: Exception) {}
        }
        vibrate(14L)
    }

    fun playSuccess() {
        if (soundEnabled) {
            try { getTone()?.startTone(ToneGenerator.TONE_PROP_ACK, 150) } catch (_: Exception) {}
        }
        vibrate(35L)
    }

    fun playError() {
        if (soundEnabled) {
            try { getTone()?.startTone(ToneGenerator.TONE_PROP_NACK, 140) } catch (_: Exception) {}
        }
        vibrate(50L)
    }

    fun playCelebration() {
        if (soundEnabled) {
            try { getTone()?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 260) } catch (_: Exception) {}
        }
        vibrate(65L)
    }

    private fun vibrate(ms: Long) {
        if (!hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(ms)
                }
            }
        } catch (_: Exception) {
        }
    }
}

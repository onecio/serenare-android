package com.serenare.core.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.serenare.domain.model.HapticIntensity
import com.serenare.domain.model.HapticPattern
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface HapticEngine {
    fun trigger(pattern: HapticPattern)
    fun setIntensity(level: HapticIntensity)
    fun isAvailable(): Boolean
}

@Singleton
class AndroidHapticEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : HapticEngine {
    private var intensity = HapticIntensity.MEDIUM
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    override fun trigger(pattern: HapticPattern) {
        if (!isAvailable() || intensity == HapticIntensity.DISABLED) return
        val amplitude = when (intensity) {
            HapticIntensity.WEAK -> 45
            HapticIntensity.MEDIUM -> 95
            HapticIntensity.STRONG -> 180
            HapticIntensity.DISABLED -> 0
        }
        val timings = when (pattern) {
            HapticPattern.BREATH_IN -> longArrayOf(0, 80, 60, 120)
            HapticPattern.BREATH_OUT -> longArrayOf(0, 120, 70, 80)
            HapticPattern.GROUNDING_PULSE -> longArrayOf(0, 45)
            HapticPattern.ANCHOR -> longArrayOf(0, 70, 50, 70)
            HapticPattern.RELEASE_ANGER -> longArrayOf(0, 180)
            HapticPattern.SUCCESS -> longArrayOf(0, 60, 60, 120)
            HapticPattern.ATTENTION -> longArrayOf(0, 90)
            HapticPattern.FOCUS_SUBTLE -> longArrayOf(0, 35)
        }
        val amplitudes = IntArray(timings.size) { index -> if (index == 0) 0 else amplitude }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(timings, -1)
        }
    }

    override fun setIntensity(level: HapticIntensity) {
        intensity = level
    }

    override fun isAvailable(): Boolean = vibrator?.hasVibrator() == true
}

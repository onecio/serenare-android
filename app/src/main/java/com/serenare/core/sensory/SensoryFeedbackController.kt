package com.serenare.core.sensory

import com.serenare.core.audio.BreathingSoundEngine
import com.serenare.core.audio.FeedbackToneEngine
import com.serenare.core.haptic.HapticEngine
import com.serenare.domain.model.HapticPattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensoryFeedbackController @Inject constructor(
    private val hapticEngine: HapticEngine,
    private val breathingSoundEngine: BreathingSoundEngine,
    private val feedbackToneEngine: FeedbackToneEngine
) {
    fun breathIn() {
        hapticEngine.trigger(HapticPattern.BREATH_IN)
        breathingSoundEngine.inhale()
    }

    fun breathOut() {
        hapticEngine.trigger(HapticPattern.BREATH_OUT)
        breathingSoundEngine.exhale()
    }

    fun groundingPulse() {
        hapticEngine.trigger(HapticPattern.GROUNDING_PULSE)
        breathingSoundEngine.groundingTone()
    }

    fun transition() {
        hapticEngine.trigger(HapticPattern.ATTENTION)
        feedbackToneEngine.transition()
    }

    fun success() {
        hapticEngine.trigger(HapticPattern.SUCCESS)
        breathingSoundEngine.success()
    }

    fun releaseAnger() {
        hapticEngine.trigger(HapticPattern.RELEASE_ANGER)
    }
}

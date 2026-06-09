package com.serenare.domain.model

import kotlinx.serialization.Serializable

enum class ModuleType(val label: String, val estimatedTime: String) {
    CRISIS_NOW("Crise agora", "3 a 5 min"),
    PANIC("Crise de panico", "3 a 5 min"),
    ANGER("Descarga segura", "4 a 6 min"),
    DAILY("Apoio diario", "3 min"),
    DECISION("Clareza decisoria", "5 min"),
    FOCUS("Foco profundo", "25 min"),
    SLEEP("Dormir", "15 a 60 min")
}

enum class SensoryMode { VOICE, VISUAL, TACTILE }

enum class BreathingPhase(val label: String) {
    INHALE("Inspire"),
    HOLD("Sustente"),
    EXHALE("Expire"),
    PAUSE("Pause")
}

enum class SoundscapeType(val label: String, val procedural: Boolean = true) {
    RAIN("Chuva"),
    OCEAN("Oceano"),
    FOREST("Floresta"),
    WIND("Vento leve"),
    WHITE_NOISE("Ruido branco"),
    BROWN_NOISE("Ruido marrom"),
    GRAY_NOISE("Ruido cinza"),
    PINK_NOISE("Ruido rosa"),
    FOCUS_DRONE("Drone de foco"),
    BREATH_IN("Inspiracao guiada"),
    BREATH_OUT("Expiracao guiada"),
    SUCCESS("Conclusao"),
    TRANSITION("Transicao"),
    GROUNDING_TONE("Tom de ancoragem")
}

enum class CrisisRiskLevel { LOW, MODERATE, HIGH, IMMEDIATE_HELP }

enum class HapticPattern {
    BREATH_IN,
    BREATH_OUT,
    GROUNDING_PULSE,
    ANCHOR,
    RELEASE_ANGER,
    SUCCESS,
    ATTENTION,
    FOCUS_SUBTLE
}

enum class HapticIntensity { WEAK, MEDIUM, STRONG, DISABLED }

@Serializable
data class SessionProgress(
    val module: ModuleType,
    val stepIndex: Int = 0,
    val totalSteps: Int = 1,
    val initialIntensity: Int? = null,
    val currentIntensity: Int? = null,
    val completed: Boolean = false,
    val startedAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null,
    val soundscape: SoundscapeType? = null
)

@Serializable
data class GroundingStep(
    val title: String,
    val instruction: String,
    val completed: Boolean = false
)

@Serializable
data class FocusTask(
    val id: String,
    val title: String,
    val reason: String = "",
    val steps: List<String> = emptyList(),
    val mentalDump: List<String> = emptyList(),
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Serializable
data class DailySupportEntry(
    val date: String,
    val energy: String,
    val achievement: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Serializable
data class SensoryProfile(
    val preferredSounds: List<SoundscapeType> = listOf(SoundscapeType.RAIN, SoundscapeType.BROWN_NOISE),
    val uncomfortableSounds: List<SoundscapeType> = emptyList(),
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val sensoryMode: SensoryMode = SensoryMode.VISUAL,
    val favoriteTechniques: List<String> = emptyList(),
    val personalAnchor: String = "",
    val supportContacts: List<String> = emptyList(),
    val primaryGoal: ModuleType = ModuleType.CRISIS_NOW
)

@Serializable
data class SessionRecord(
    val module: ModuleType,
    val startedAtEpochMs: Long,
    val durationMs: Long,
    val initialIntensity: Int? = null,
    val finalIntensity: Int? = null,
    val soundscape: SoundscapeType? = null,
    val efficacy: Int? = null
)

@Serializable
data class PomodoroState(
    val taskTitle: String = "",
    val currentStep: String = "",
    val remainingSeconds: Int = 0,
    val running: Boolean = false,
    val workSeconds: Int = 25 * 60,
    val breakSeconds: Int = 5 * 60
)

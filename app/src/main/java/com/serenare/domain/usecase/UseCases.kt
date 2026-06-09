package com.serenare.domain.usecase

import com.serenare.data.repository.AudioRepository
import com.serenare.data.repository.GeminiRepository
import com.serenare.data.repository.SafetyRepository
import com.serenare.data.repository.SessionRepository
import com.serenare.domain.model.DailySupportEntry
import com.serenare.domain.model.FocusTask
import com.serenare.domain.model.ModuleType
import com.serenare.domain.model.SoundscapeType
import javax.inject.Inject

class StartModuleSessionUseCase @Inject constructor() {
    operator fun invoke(): Long = System.currentTimeMillis()
}

class CompleteModuleSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val geminiRepository: GeminiRepository
) {
    suspend operator fun invoke(
        module: ModuleType,
        startedAt: Long,
        initialIntensity: Int?,
        finalIntensity: Int?,
        soundscape: SoundscapeType?
    ): String {
        sessionRepository.complete(module, startedAt, initialIntensity, finalIntensity, soundscape)
        return geminiRepository.closingAffirmation(module, initialIntensity, finalIntensity)
    }
}

class SaveSessionProgressUseCase @Inject constructor()

class GenerateGeminiGuidanceUseCase @Inject constructor(
    private val geminiRepository: GeminiRepository
) {
    suspend fun angerQuestions() = geminiRepository.angerQuestions()
    suspend fun focusSteps(task: String) = geminiRepository.focusSteps(task)
    suspend fun socraticQuestion(pros: List<String>, cons: List<String>) = geminiRepository.socraticQuestion(pros, cons)
    suspend fun neutralSummary(pros: List<String>, cons: List<String>) = geminiRepository.neutralSummary(pros, cons)
    suspend fun dailyAction() = geminiRepository.dailyAction()
}

class DetectCrisisRiskUseCase @Inject constructor(
    private val safetyRepository: SafetyRepository
) {
    operator fun invoke(intensity: Int): Boolean = safetyRepository.riskForIntensity(intensity)
}

class StartBreathingProtocolUseCase @Inject constructor()

class StartSoundscapeUseCase @Inject constructor(
    private val audioRepository: AudioRepository
) {
    operator fun invoke(type: SoundscapeType) = audioRepository.switchTo(type)
}

class SaveDailyAchievementUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(entry: DailySupportEntry) = sessionRepository.saveDaily(entry)
}

class GenerateFallbackContentUseCase @Inject constructor()

class SaveFocusTaskUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(task: FocusTask) = sessionRepository.saveFocus(task)
}

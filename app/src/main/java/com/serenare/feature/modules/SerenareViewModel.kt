package com.serenare.feature.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenare.core.audio.AudioEngine
import com.serenare.core.sensory.SensoryFeedbackController
import com.serenare.data.local.PreferencesStore
import com.serenare.data.repository.ContentRepository
import com.serenare.domain.model.DailySupportEntry
import com.serenare.domain.model.FocusTask
import com.serenare.domain.model.ModuleType
import com.serenare.domain.model.PomodoroState
import com.serenare.domain.model.SoundscapeType
import com.serenare.domain.usecase.CompleteModuleSessionUseCase
import com.serenare.domain.usecase.GenerateGeminiGuidanceUseCase
import com.serenare.domain.usecase.SaveDailyAchievementUseCase
import com.serenare.domain.usecase.SaveFocusTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModuleUiState(
    val activeModule: ModuleType = ModuleType.CRISIS_NOW,
    val step: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val initialIntensity: Int? = null,
    val finalIntensity: Int? = null,
    val selectedSound: SoundscapeType? = null,
    val guidance: String = "",
    val loading: Boolean = false,
    val completed: Boolean = false,
    val tags: List<String> = emptyList(),
    val taskTitle: String = "",
    val taskReason: String = "",
    val focusSteps: List<String> = emptyList(),
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val dailyEnergy: String = "",
    val dailyAchievement: String = "",
    val mentalDump: List<String> = emptyList()
)

@HiltViewModel
class SerenareViewModel @Inject constructor(
    private val completeSession: CompleteModuleSessionUseCase,
    private val guidanceUseCase: GenerateGeminiGuidanceUseCase,
    private val saveDaily: SaveDailyAchievementUseCase,
    private val saveFocusTask: SaveFocusTaskUseCase,
    private val contentRepository: ContentRepository,
    private val audioEngine: AudioEngine,
    private val sensoryFeedbackController: SensoryFeedbackController,
    preferencesStore: PreferencesStore
) : ViewModel() {
    private val _state = MutableStateFlow(ModuleUiState())
    val state = _state.asStateFlow()
    val sessions = preferencesStore.sessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dailyStreak = preferencesStore.dailyStreak.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val pomodoro = preferencesStore.pomodoroState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PomodoroState())
    private val _library = MutableStateFlow<Map<String, String>>(emptyMap())
    val library: StateFlow<Map<String, String>> = _library.asStateFlow()
    private var pomodoroJob: Job? = null

    fun start(module: ModuleType) {
        _state.value = ModuleUiState(activeModule = module, startedAt = System.currentTimeMillis())
        sensoryFeedbackController.transition()
    }

    fun setIntensity(value: Int, final: Boolean = false) {
        _state.value = if (final) _state.value.copy(finalIntensity = value) else _state.value.copy(initialIntensity = value)
    }

    fun next(totalSteps: Int) {
        val current = _state.value
        if (current.step + 1 >= totalSteps) {
            finish()
        } else {
            _state.value = current.copy(step = current.step + 1)
            sensoryFeedbackController.transition()
        }
    }

    fun previous() {
        val current = _state.value
        _state.value = current.copy(step = (current.step - 1).coerceAtLeast(0))
    }

    fun selectSound(sound: SoundscapeType) {
        audioEngine.crossfadeTo(sound)
        _state.value = _state.value.copy(selectedSound = sound)
    }

    fun stopAudio() = audioEngine.stopAll()

    fun breathIn() = sensoryFeedbackController.breathIn()
    fun breathOut() = sensoryFeedbackController.breathOut()
    fun groundingPulse() = sensoryFeedbackController.groundingPulse()
    fun success() = sensoryFeedbackController.success()
    fun releaseAnger() = sensoryFeedbackController.releaseAnger()

    fun toggleTag(value: String) {
        val tags = _state.value.tags
        _state.value = _state.value.copy(tags = if (value in tags) tags - value else tags + value)
    }

    fun setTask(title: String, reason: String) {
        _state.value = _state.value.copy(taskTitle = title, taskReason = reason)
    }

    fun setDaily(energy: String? = null, achievement: String? = null) {
        _state.value = _state.value.copy(
            dailyEnergy = energy ?: _state.value.dailyEnergy,
            dailyAchievement = achievement ?: _state.value.dailyAchievement
        )
    }

    fun addPro(value: String) {
        if (value.isNotBlank()) _state.value = _state.value.copy(pros = _state.value.pros + value.trim())
    }

    fun addCon(value: String) {
        if (value.isNotBlank()) _state.value = _state.value.copy(cons = _state.value.cons + value.trim())
    }

    fun addMentalDump(value: String) {
        if (value.isNotBlank()) _state.value = _state.value.copy(mentalDump = _state.value.mentalDump + value.trim())
    }

    fun generateForCurrentStep() {
        val current = _state.value
        _state.value = current.copy(loading = true)
        viewModelScope.launch {
            val result = when (current.activeModule) {
                ModuleType.ANGER -> guidanceUseCase.angerQuestions().joinToString("\n")
                ModuleType.FOCUS -> {
                    val steps = guidanceUseCase.focusSteps(current.taskTitle.ifBlank { "tarefa principal" })
                    _state.value = _state.value.copy(focusSteps = steps)
                    steps.joinToString("\n")
                }
                ModuleType.DECISION -> guidanceUseCase.neutralSummary(current.pros, current.cons) + "\n" + guidanceUseCase.socraticQuestion(current.pros, current.cons)
                ModuleType.DAILY -> guidanceUseCase.dailyAction()
                else -> ""
            }
            _state.value = _state.value.copy(guidance = result, loading = false)
        }
    }

    fun finish() {
        val current = _state.value
        _state.value = current.copy(loading = true)
        viewModelScope.launch {
            if (current.activeModule == ModuleType.DAILY && current.dailyAchievement.isNotBlank()) {
                saveDaily(DailySupportEntry(LocalDate.now().toString(), current.dailyEnergy, current.dailyAchievement))
            }
            if (current.activeModule == ModuleType.FOCUS && current.taskTitle.isNotBlank()) {
                saveFocusTask(
                    FocusTask(
                        id = UUID.randomUUID().toString(),
                        title = current.taskTitle,
                        reason = current.taskReason,
                        steps = current.focusSteps,
                        mentalDump = current.mentalDump
                    )
                )
            }
            val message = completeSession(
                current.activeModule,
                current.startedAt,
                current.initialIntensity,
                current.finalIntensity,
                current.selectedSound
            )
            audioEngine.stopAll()
            sensoryFeedbackController.success()
            _state.value = current.copy(guidance = message, loading = false, completed = true)
        }
    }

    fun loadLibrary(topics: List<String>) {
        viewModelScope.launch {
            topics.forEach { topic ->
                if (_library.value[topic] == null) {
                    _library.value = _library.value + (topic to contentRepository.topic(topic))
                }
            }
        }
    }

    fun startPomodoro(workSeconds: Int, breakSeconds: Int) {
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            var remaining = workSeconds
            while (remaining >= 0) {
                delay(1_000)
                remaining--
            }
        }
    }

    override fun onCleared() {
        audioEngine.release()
        super.onCleared()
    }
}

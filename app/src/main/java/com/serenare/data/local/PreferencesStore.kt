package com.serenare.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.serenare.domain.model.DailySupportEntry
import com.serenare.domain.model.FocusTask
import com.serenare.domain.model.HapticIntensity
import com.serenare.domain.model.ModuleType
import com.serenare.domain.model.PomodoroState
import com.serenare.domain.model.SensoryProfile
import com.serenare.domain.model.SessionRecord
import com.serenare.domain.model.SoundscapeType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.serenareDataStore by preferencesDataStore(name = "serenare_preferences")

@Singleton
class PreferencesStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val sensoryProfile = stringPreferencesKey("sensory_profile")
        val volumeBreathing = floatPreferencesKey("volume_breathing")
        val volumeAmbient = floatPreferencesKey("volume_ambient")
        val volumeFeedback = floatPreferencesKey("volume_feedback")
        val hapticEnabled = booleanPreferencesKey("haptic_enabled")
        val hapticIntensity = stringPreferencesKey("haptic_intensity")
        val dailyStreak = intPreferencesKey("daily_streak")
        val lastSessionDate = stringPreferencesKey("last_session_date")
        val dailyAchievements = stringPreferencesKey("daily_achievements")
        val focusTasks = stringPreferencesKey("focus_tasks")
        val mentalDumpNotes = stringPreferencesKey("mental_dump_notes")
        val libraryCache = stringPreferencesKey("library_cache")
        val lastModuleUsed = stringPreferencesKey("last_module_used")
        val sessions = stringPreferencesKey("sessions")
        val pomodoroState = stringPreferencesKey("pomodoro_state")
        val prudentAction = stringPreferencesKey("prudent_action")
    }

    val sensoryProfile: Flow<SensoryProfile> = context.serenareDataStore.data.map {
        decodeOrDefault(it[Keys.sensoryProfile], SensoryProfile())
    }

    val sessions: Flow<List<SessionRecord>> = context.serenareDataStore.data.map {
        decodeOrDefault(it[Keys.sessions], emptyList())
    }

    val pomodoroState: Flow<PomodoroState> = context.serenareDataStore.data.map {
        decodeOrDefault(it[Keys.pomodoroState], PomodoroState())
    }

    val dailyStreak: Flow<Int> = context.serenareDataStore.data.map { it[Keys.dailyStreak] ?: 0 }

    suspend fun markOnboardingCompleted() {
        context.serenareDataStore.edit { it[Keys.onboardingCompleted] = true }
    }

    suspend fun saveProfile(profile: SensoryProfile) {
        context.serenareDataStore.edit {
            it[Keys.sensoryProfile] = json.encodeToString(profile)
            it[Keys.hapticIntensity] = profile.hapticIntensity.name
        }
    }

    suspend fun setVolume(type: SoundscapeType, volume: Float) {
        val key = when (type) {
            SoundscapeType.BREATH_IN, SoundscapeType.BREATH_OUT -> Keys.volumeBreathing
            SoundscapeType.SUCCESS, SoundscapeType.TRANSITION, SoundscapeType.GROUNDING_TONE -> Keys.volumeFeedback
            else -> Keys.volumeAmbient
        }
        context.serenareDataStore.edit { it[key] = volume.coerceIn(0f, 1f) }
    }

    suspend fun saveSession(record: SessionRecord) {
        val current = sessions.first()
        context.serenareDataStore.edit {
            it[Keys.sessions] = json.encodeToString((current + record).takeLast(200))
            it[Keys.lastModuleUsed] = record.module.name
            it[Keys.lastSessionDate] = record.startedAtEpochMs.toString()
        }
    }

    suspend fun saveDailyAchievement(entry: DailySupportEntry) {
        val current = context.serenareDataStore.data.map {
            decodeOrDefault<List<DailySupportEntry>>(it[Keys.dailyAchievements], emptyList())
        }.first()
        context.serenareDataStore.edit {
            it[Keys.dailyAchievements] = json.encodeToString((current + entry).takeLast(90))
            it[Keys.dailyStreak] = (it[Keys.dailyStreak] ?: 0) + 1
        }
    }

    suspend fun saveFocusTask(task: FocusTask) {
        val current = context.serenareDataStore.data.map {
            decodeOrDefault<List<FocusTask>>(it[Keys.focusTasks], emptyList())
        }.first()
        context.serenareDataStore.edit { it[Keys.focusTasks] = json.encodeToString((current + task).takeLast(40)) }
    }

    suspend fun saveMentalDump(notes: List<String>) {
        context.serenareDataStore.edit { it[Keys.mentalDumpNotes] = json.encodeToString(notes.takeLast(200)) }
    }

    suspend fun saveLibraryTopic(topic: String, content: String) {
        val current = libraryCache()
        context.serenareDataStore.edit { it[Keys.libraryCache] = json.encodeToString(current + (topic to content)) }
    }

    suspend fun libraryCache(): Map<String, String> {
        return context.serenareDataStore.data.map {
            decodeOrDefault<Map<String, String>>(it[Keys.libraryCache], emptyMap())
        }.first()
    }

    suspend fun savePomodoro(state: PomodoroState) {
        context.serenareDataStore.edit { it[Keys.pomodoroState] = json.encodeToString(state) }
    }

    suspend fun savePrudentAction(value: String) {
        context.serenareDataStore.edit { it[Keys.prudentAction] = value }
    }

    suspend fun clearAll() {
        context.serenareDataStore.edit { it.clear() }
    }

    private inline fun <reified T> decodeOrDefault(raw: String?, fallback: T): T {
        return raw?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() } ?: fallback
    }
}

package com.serenare.data.repository

import com.serenare.BuildConfig
import com.serenare.core.audio.AudioEngine
import com.serenare.core.haptic.HapticEngine
import com.serenare.data.local.PreferencesStore
import com.serenare.domain.model.DailySupportEntry
import com.serenare.domain.model.FocusTask
import com.serenare.domain.model.HapticPattern
import com.serenare.domain.model.ModuleType
import com.serenare.domain.model.SessionRecord
import com.serenare.domain.model.SoundscapeType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AudioRepository @Inject constructor(
    private val audioEngine: AudioEngine
) {
    fun start(type: SoundscapeType) = audioEngine.play(type, loop = true)
    fun feedback(type: SoundscapeType) = audioEngine.play(type, loop = false)
    fun switchTo(type: SoundscapeType) = audioEngine.crossfadeTo(type)
    fun volume(type: SoundscapeType, volume: Float) = audioEngine.setVolume(type, volume)
    fun stopAll() = audioEngine.stopAll()
}

@Singleton
class HapticRepository @Inject constructor(
    private val hapticEngine: HapticEngine
) {
    fun trigger(pattern: HapticPattern) = hapticEngine.trigger(pattern)
}

@Singleton
class SafetyRepository @Inject constructor() {
    fun riskForIntensity(intensity: Int): Boolean = intensity >= 8
    fun containsImmediateRisk(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("suicidio", "suicídio", "me matar", "autoagressao", "autoagressão", "violencia", "violência")
            .any(normalized::contains)
    }
}

@Singleton
class SessionRepository @Inject constructor(
    private val preferencesStore: PreferencesStore
) {
    val sessions = preferencesStore.sessions

    suspend fun complete(
        module: ModuleType,
        startedAt: Long,
        initialIntensity: Int?,
        finalIntensity: Int?,
        soundscape: SoundscapeType?
    ) {
        preferencesStore.saveSession(
            SessionRecord(
                module = module,
                startedAtEpochMs = startedAt,
                durationMs = System.currentTimeMillis() - startedAt,
                initialIntensity = initialIntensity,
                finalIntensity = finalIntensity,
                soundscape = soundscape
            )
        )
    }

    suspend fun saveDaily(entry: DailySupportEntry) = preferencesStore.saveDailyAchievement(entry)
    suspend fun saveFocus(task: FocusTask) = preferencesStore.saveFocusTask(task)
}

@Singleton
class GeminiRepository @Inject constructor(
    private val safetyRepository: SafetyRepository
) {
    private val client = OkHttpClient.Builder().build()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun closingAffirmation(module: ModuleType, initial: Int?, current: Int?): String {
        val fallback = when (module) {
            ModuleType.CRISIS_NOW -> "A intensidade foi observada e reduzida por etapas concretas. Continue apenas no proximo minuto."
            ModuleType.PANIC -> "Volte para este minuto. O corpo esta ativado, e voce esta orientando a resposta agora."
            ModuleType.ANGER -> "Voce reduziu a chance de agir no impulso. Esta pausa protege o que importa."
            ModuleType.DAILY -> "Isso conta. Mesmo que pareca pouco."
            ModuleType.DECISION -> "Uma decisao prudente pode aguardar dados mais claros."
            ModuleType.FOCUS -> "O proximo passo ficou menor e executavel."
            ModuleType.SLEEP -> "A preocupacao foi registrada. Ela pode ficar aqui por agora."
        }
        return generateText(
            prompt = "Intensidade inicial $initial, atual $current. Gere afirmacao curta de encerramento. Maximo 2 frases. PT-BR. Tom sereno, concreto, nao condescendente. Sem cliches.",
            fallback = fallback
        )
    }

    suspend fun angerQuestions(): List<String> {
        return generateJsonArray(
            prompt = "Gere exatamente 3 perguntas reflexivas para pessoa com raiva intensa. Sobre valores, consequencias e perspectiva futura. Nao minimize. Nao culpe. Responda SOMENTE com JSON array de strings. PT-BR.",
            fallback = listOf(
                "Qual resposta preserva seus valores neste momento?",
                "O que pode mudar se voce aguardar dez minutos?",
                "Que escolha reduz danos para o seu futuro imediato?"
            )
        ).take(3)
    }

    suspend fun focusSteps(task: String): List<String> {
        return generateJsonArray(
            prompt = "Quebre \"$task\" em exatamente 3 passos de acao, maximo 10 palavras cada. Responda SOMENTE com JSON array de strings. Sem preamble.",
            fallback = listOf("Abra o material necessario", "Complete a primeira parte menor", "Revise o que foi feito")
        ).take(3)
    }

    suspend fun socraticQuestion(pros: List<String>, cons: List<String>): String {
        return generateText(
            prompt = "Pros: $pros. Contras: $cons. Gere UMA pergunta socratica sobre valores, consequencias de longo prazo OU reversibilidade. Maximo 20 palavras. PT-BR. Sem julgamento.",
            fallback = "Esta escolha ainda parece prudente se voce precisar sustenta-la por uma semana?"
        )
    }

    suspend fun neutralSummary(pros: List<String>, cons: List<String>): String {
        return generateText(
            prompt = "Pros: $pros. Contras: $cons. Gere sintese neutra em 2 frases, PT-BR, sem julgamento.",
            fallback = "A decisao contem beneficios praticos e custos que precisam ser reconhecidos. O proximo passo prudente e reduzir irreversibilidade."
        )
    }

    suspend fun dailyAction(): String {
        return generateText(
            prompt = "Gere 1 acao pequena de autocuidado concreto para hoje. PT-BR. Sem promessa de melhora.",
            fallback = "Beba um copo de agua agora."
        )
    }

    suspend fun knowledge(topic: String): String {
        return generateText(
            prompt = "Escreva sobre $topic em 4 paragrafos: o que e, como aparece no corpo/comportamento, estrategia pratica imediata, quando buscar ajuda profissional. Nao diagnostique. Sem markdown. PT-BR. Linguagem clara, adulta, respeitosa.",
            fallback = KnowledgeFallbacks.topic(topic)
        )
    }

    private suspend fun generateText(prompt: String, fallback: String): String = withContext(Dispatchers.IO) {
        if (BuildConfig.GEMINI_API_KEY.isBlank() || safetyRepository.containsImmediateRisk(prompt)) return@withContext fallback
        runCatching {
            val payload = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", safetyPrompt + "\n\n" + prompt)))))
                .toString()
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
                .post(payload.toRequestBody(mediaType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use fallback
                val root = JSONObject(response.body?.string().orEmpty())
                root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.replace("**", "")
                    ?.replace("##", "")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: fallback
            }
        }.getOrElse { fallback }
    }

    private suspend fun generateJsonArray(prompt: String, fallback: List<String>): List<String> {
        val raw = generateText(prompt, JSONArray(fallback).toString())
        return runCatching {
            val start = raw.indexOf('[').coerceAtLeast(0)
            val end = raw.lastIndexOf(']').takeIf { it >= start } ?: raw.lastIndex
            val array = JSONArray(raw.substring(start, end + 1))
            List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
        }.getOrDefault(fallback)
    }

    private val safetyPrompt = """
        Voce e um assistente de apoio emocional e autorregulacao. Nao e terapeuta, medico nem servico de emergencia.
        Nao faca diagnostico. Nao recomende medicamentos. Nao incentive isolamento ou decisoes impulsivas.
        Se identificar risco de autoagressao, suicidio, violencia ou emergencia medica, oriente imediatamente buscar ajuda: CVV 188 ou SAMU 192.
    """.trimIndent()
}

@Singleton
class ContentRepository @Inject constructor(
    private val preferencesStore: PreferencesStore,
    private val geminiRepository: GeminiRepository
) {
    suspend fun topic(topic: String): String {
        val cache = preferencesStore.libraryCache()
        cache[topic]?.let { return it }
        val generated = geminiRepository.knowledge(topic)
        preferencesStore.saveLibraryTopic(topic, generated)
        return generated
    }
}

object KnowledgeFallbacks {
    fun topic(topic: String): String = when (topic.lowercase()) {
        "ansiedade" -> "Ansiedade e uma resposta de alerta que pode surgir quando o corpo interpreta uma situacao como exigente ou incerta.\n\nNo corpo, pode aparecer como respiracao curta, tensao, agitacao, aperto no peito ou dificuldade de manter atencao.\n\nUma estrategia imediata e alongar a expiracao: inspire por tres segundos e expire por seis, repetindo por quatro ciclos.\n\nBusque ajuda profissional quando a intensidade for frequente, limitar atividades importantes ou vier acompanhada de risco de autoagressao."
        "ataque de panico" -> "Ataque de panico e uma ativacao intensa e rapida do sistema de alarme do corpo, sem significar diagnostico por si so.\n\nPode aparecer como taquicardia, tremor, suor, medo de perder controle, falta de ar ou sensacao de irrealidade.\n\nUma estrategia imediata e orientar-se pelo ambiente: nomeie o lugar, pressione os pes no chao e acompanhe a respiracao quadrada.\n\nBusque ajuda imediata se houver risco fisico, dor intensa, desmaio, ideia de autoagressao ou incerteza sobre seguranca."
        "regulacao da raiva" -> "Regulacao da raiva e a capacidade de reduzir ativacao antes de responder a algo percebido como injusto, ameacador ou frustrante.\n\nPode aparecer como calor, rigidez mandibular, impulsividade, voz elevada, vontade de atacar ou interromper contato.\n\nUma estrategia imediata e pressionar as maos por tres segundos, soltar lentamente e aguardar dez minutos antes de decidir.\n\nBusque ajuda profissional quando explosoes forem recorrentes, causarem danos, envolverem violencia ou colocarem alguem em risco."
        "apoio em episodio depressivo" -> "Apoio diario em episodio depressivo foca microacoes concretas, sem substituir tratamento e sem exigir grande energia.\n\nPode aparecer como lentidao, isolamento, perda de interesse, cansaco, autocritica e dificuldade para iniciar tarefas.\n\nUma estrategia imediata e escolher uma acao pequena e verificavel, como beber agua, levantar ou alongar os dedos por trinta segundos.\n\nBusque ajuda profissional se houver sofrimento persistente, prejuizo importante, abandono de autocuidado ou pensamentos de morte."
        "decisao sob estresse" -> "Decisao sob estresse ocorre quando urgencia emocional reduz a capacidade de avaliar consequencias, alternativas e reversibilidade.\n\nPode aparecer como pressa, pensamento tudo-ou-nada, irritacao, medo de perder oportunidade ou vontade de resolver tudo imediatamente.\n\nUma estrategia imediata e perguntar se a decisao precisa ser tomada agora e listar pros e contras antes de agir.\n\nBusque apoio profissional ou de uma pessoa confiavel quando a decisao envolver seguranca, saude, violencia, dinheiro relevante ou risco legal."
        else -> "Procrastinacao e foco envolvem iniciar e sustentar atencao em uma tarefa apesar de distracoes internas ou externas.\n\nPode aparecer como alternancia entre tarefas, evitacao, busca de estimulo, cansaco decisorio e dificuldade para definir o primeiro passo.\n\nUma estrategia imediata e quebrar a tarefa em tres passos pequenos e iniciar por dois minutos com som constante de fundo.\n\nBusque ajuda profissional quando a dificuldade for persistente, causar prejuizo relevante ou vier associada a sofrimento intenso."
    }
}

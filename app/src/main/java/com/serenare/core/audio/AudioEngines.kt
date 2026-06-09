package com.serenare.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import androidx.annotation.FloatRange
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.serenare.domain.model.SoundscapeType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

interface AudioEngine {
    fun play(type: SoundscapeType, loop: Boolean = false)
    fun stop(type: SoundscapeType)
    fun setVolume(type: SoundscapeType, volume: Float)
    fun crossfadeTo(type: SoundscapeType, durationMs: Long = 800)
    fun stopAll()
    fun release()
}

class LifecycleAudioObserver(private val audioEngine: AudioEngine) : DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) = audioEngine.stopAll()
    override fun onDestroy(owner: LifecycleOwner) = audioEngine.release()
}

@Singleton
class ProceduralNoiseEngine @Inject constructor() {
    private val active = ConcurrentHashMap<SoundscapeType, AudioTrack>()
    private val volumes = ConcurrentHashMap<SoundscapeType, Float>()
    @Volatile private var released = false

    fun play(type: SoundscapeType, loop: Boolean = true) {
        stop(type)
        released = false
        thread(name = "serenare-audio-${type.name.lowercase()}", isDaemon = true) {
            val sampleRate = 44_100
            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(sampleRate / 2)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            active[type] = track
            track.play()
            var brown = 0.0
            var grayPhase = 0.0
            var elapsed = 0
            val random = Random(type.ordinal + System.nanoTime().toInt())
            val buffer = ShortArray(minBuffer / 2)
            while (!released && active[type] === track) {
                for (i in buffer.indices) {
                    val seconds = (elapsed + i).toDouble() / sampleRate
                    val raw = when (type) {
                        SoundscapeType.BROWN_NOISE -> {
                            brown = (brown + random.nextDouble(-0.05, 0.05)).coerceIn(-1.0, 1.0)
                            brown
                        }
                        SoundscapeType.GRAY_NOISE -> {
                            grayPhase += 2.0 * PI * 92.0 / sampleRate
                            random.nextDouble(-0.5, 0.5) + sin(grayPhase) * 0.15
                        }
                        SoundscapeType.PINK_NOISE -> random.nextDouble(-1.0, 1.0) * 0.6 + brown * 0.4
                        SoundscapeType.FOCUS_DRONE -> sin(2.0 * PI * 40.0 * seconds) * (0.45 + 0.15 * sin(2.0 * PI * 0.25 * seconds))
                        SoundscapeType.RAIN -> random.nextDouble(-0.7, 0.7) * (0.45 + random.nextDouble() * 0.25)
                        SoundscapeType.OCEAN -> sin(2.0 * PI * 0.16 * seconds) * 0.55 + random.nextDouble(-0.18, 0.18)
                        SoundscapeType.FOREST -> sin(2.0 * PI * 3.0 * seconds) * 0.06 + random.nextDouble(-0.22, 0.22)
                        SoundscapeType.WIND -> sin(2.0 * PI * 0.08 * seconds) * 0.35 + random.nextDouble(-0.12, 0.12)
                        SoundscapeType.BREATH_IN -> sin(2.0 * PI * 220.0 * seconds) * ((seconds % 3.0) / 3.0)
                        SoundscapeType.BREATH_OUT -> sin(2.0 * PI * 146.0 * seconds) * (1.0 - ((seconds % 6.0) / 6.0))
                        SoundscapeType.SUCCESS -> sin(2.0 * PI * 660.0 * seconds) * 0.35
                        SoundscapeType.TRANSITION -> sin(2.0 * PI * 330.0 * seconds) * 0.25
                        SoundscapeType.GROUNDING_TONE -> sin(2.0 * PI * 196.0 * seconds) * 0.3
                        SoundscapeType.WHITE_NOISE -> random.nextDouble(-1.0, 1.0)
                    }
                    buffer[i] = (raw * Short.MAX_VALUE * (volumes[type] ?: 0.32f)).toInt().toShort()
                }
                track.write(buffer, 0, buffer.size)
                elapsed += buffer.size
                if (!loop && elapsed > sampleRate) break
            }
            track.stopSafely()
            active.remove(type, track)
        }
    }

    fun stop(type: SoundscapeType) {
        active.remove(type)?.stopSafely()
    }

    fun setVolume(type: SoundscapeType, @FloatRange(from = 0.0, to = 1.0) volume: Float) {
        volumes[type] = volume.coerceIn(0f, 1f)
    }

    fun stopAll() {
        active.keys.toList().forEach(::stop)
    }

    fun release() {
        released = true
        stopAll()
    }

    private fun AudioTrack.stopSafely() {
        runCatching {
            pause()
            flush()
            release()
        }
    }
}

@Singleton
class SoundscapeController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proceduralNoiseEngine: ProceduralNoiseEngine
) : AudioEngine {
    private val players = ConcurrentHashMap<SoundscapeType, ExoPlayer>()
    private val volumes = ConcurrentHashMap<SoundscapeType, Float>()
    private val remoteUrls = emptyMap<SoundscapeType, String>()

    override fun play(type: SoundscapeType, loop: Boolean) {
        val remote = remoteUrls[type]
        if (remote.isNullOrBlank()) {
            proceduralNoiseEngine.play(type, loop)
            return
        }
        runCatching {
            stop(type)
            val player = ExoPlayer.Builder(context).build().also {
                it.setMediaItem(MediaItem.fromUri(remote))
                it.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                it.volume = volumes[type] ?: 0.45f
                it.prepare()
                it.playWhenReady = true
            }
            players[type] = player
        }.onFailure {
            proceduralNoiseEngine.play(type, loop)
        }
    }

    override fun stop(type: SoundscapeType) {
        players.remove(type)?.release()
        proceduralNoiseEngine.stop(type)
    }

    override fun setVolume(type: SoundscapeType, volume: Float) {
        val safe = volume.coerceIn(0f, 1f)
        volumes[type] = safe
        players[type]?.volume = safe
        proceduralNoiseEngine.setVolume(type, safe)
    }

    override fun crossfadeTo(type: SoundscapeType, durationMs: Long) {
        val current = players.keys.toList()
        current.forEach { stop(it) }
        proceduralNoiseEngine.stopAll()
        play(type, true)
    }

    override fun stopAll() {
        players.keys.toList().forEach(::stop)
        proceduralNoiseEngine.stopAll()
    }

    override fun release() {
        stopAll()
        players.clear()
        proceduralNoiseEngine.release()
    }
}

@Singleton
class BreathingSoundEngine @Inject constructor(
    private val audioEngine: SoundscapeController
) {
    fun inhale() = audioEngine.play(SoundscapeType.BREATH_IN, loop = false)
    fun exhale() = audioEngine.play(SoundscapeType.BREATH_OUT, loop = false)
    fun groundingTone() = audioEngine.play(SoundscapeType.GROUNDING_TONE, loop = false)
    fun success() = audioEngine.play(SoundscapeType.SUCCESS, loop = false)
}

@Singleton
class FeedbackToneEngine @Inject constructor() {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 45)

    fun transition() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 140)
    }

    fun attention() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
    }

    fun release() {
        toneGenerator.release()
    }
}

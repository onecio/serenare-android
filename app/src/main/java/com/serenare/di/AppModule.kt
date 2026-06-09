package com.serenare.di

import com.serenare.core.audio.AudioEngine
import com.serenare.core.audio.SoundscapeController
import com.serenare.core.haptic.AndroidHapticEngine
import com.serenare.core.haptic.HapticEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindAudioEngine(implementation: SoundscapeController): AudioEngine

    @Binds
    @Singleton
    abstract fun bindHapticEngine(implementation: AndroidHapticEngine): HapticEngine
}

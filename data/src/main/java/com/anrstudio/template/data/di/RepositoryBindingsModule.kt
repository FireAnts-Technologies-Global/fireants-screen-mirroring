package com.hyper.castwave.data.di

import com.hyper.castwave.data.repository.CastDeviceRepositoryImpl
import com.hyper.castwave.data.repository.CastSessionRepositoryImpl
import com.hyper.castwave.data.repository.ChannelRepositoryImpl
import com.hyper.castwave.data.repository.MediaRepositoryImpl
import com.hyper.castwave.data.repository.PlaybackControlRepositoryImpl
import com.hyper.castwave.data.repository.ScreenMirrorRepositoryImpl
import com.hyper.castwave.data.repository.VideoExtractorRepositoryImpl
import com.hyper.castwave.domain.repository.CastDeviceRepository
import com.hyper.castwave.domain.repository.CastSessionRepository
import com.hyper.castwave.domain.repository.ChannelRepository
import com.hyper.castwave.domain.repository.MediaRepository
import com.hyper.castwave.domain.repository.PlaybackControlRepository
import com.hyper.castwave.domain.repository.ScreenMirrorRepository
import com.hyper.castwave.domain.repository.VideoExtractorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    @Binds
    @Singleton
    abstract fun bindCastDeviceRepository(impl: CastDeviceRepositoryImpl): CastDeviceRepository

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindCastSessionRepository(impl: CastSessionRepositoryImpl): CastSessionRepository

    @Binds
    @Singleton
    abstract fun bindScreenMirrorRepository(impl: ScreenMirrorRepositoryImpl): ScreenMirrorRepository

    @Binds
    @Singleton
    abstract fun bindVideoExtractorRepository(impl: VideoExtractorRepositoryImpl): VideoExtractorRepository

    @Binds
    @Singleton
    abstract fun bindPlaybackControlRepository(impl: PlaybackControlRepositoryImpl): PlaybackControlRepository
}

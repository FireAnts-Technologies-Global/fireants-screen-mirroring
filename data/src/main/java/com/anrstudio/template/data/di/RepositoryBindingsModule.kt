package com.anrstudio.template.data.di

import com.anrstudio.template.data.repository.CastDeviceRepositoryImpl
import com.anrstudio.template.data.repository.CastSessionRepositoryImpl
import com.anrstudio.template.data.repository.ChannelRepositoryImpl
import com.anrstudio.template.data.repository.MediaRepositoryImpl
import com.anrstudio.template.data.repository.PlaybackControlRepositoryImpl
import com.anrstudio.template.data.repository.ScreenMirrorRepositoryImpl
import com.anrstudio.template.data.repository.VideoExtractorRepositoryImpl
import com.anrstudio.template.domain.repository.CastDeviceRepository
import com.anrstudio.template.domain.repository.CastSessionRepository
import com.anrstudio.template.domain.repository.ChannelRepository
import com.anrstudio.template.domain.repository.MediaRepository
import com.anrstudio.template.domain.repository.PlaybackControlRepository
import com.anrstudio.template.domain.repository.ScreenMirrorRepository
import com.anrstudio.template.domain.repository.VideoExtractorRepository
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

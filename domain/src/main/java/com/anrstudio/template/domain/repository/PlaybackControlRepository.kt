package com.anrstudio.template.domain.repository

import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.model.PlaybackState
import kotlinx.coroutines.flow.Flow

interface PlaybackControlRepository {
    fun observePlaybackState(): Flow<PlaybackState>
    suspend fun play(): DataResult<Unit>
    suspend fun pause(): DataResult<Unit>
    suspend fun seek(position: Long): DataResult<Unit>
    suspend fun setVolume(volume: Float): DataResult<Unit>
    suspend fun stop(): DataResult<Unit>
}

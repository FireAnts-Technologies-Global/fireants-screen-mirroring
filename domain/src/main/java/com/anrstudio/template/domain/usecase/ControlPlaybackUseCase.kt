package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.repository.PlaybackControlRepository
import javax.inject.Inject

class ControlPlaybackUseCase @Inject constructor(
    private val repository: PlaybackControlRepository,
) {
    suspend fun play(): DataResult<Unit> = repository.play()
    suspend fun pause(): DataResult<Unit> = repository.pause()
    suspend fun seek(position: Long): DataResult<Unit> = repository.seek(position)
    suspend fun setVolume(volume: Float): DataResult<Unit> = repository.setVolume(volume)
    suspend fun stop(): DataResult<Unit> = repository.stop()
}

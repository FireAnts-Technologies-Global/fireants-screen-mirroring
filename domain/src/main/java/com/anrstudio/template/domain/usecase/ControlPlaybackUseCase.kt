package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.repository.PlaybackControlRepository
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

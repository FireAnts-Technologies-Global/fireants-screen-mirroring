package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.PlaybackState
import com.hyper.castwave.domain.repository.PlaybackControlRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaybackStateUseCase @Inject constructor(
    private val repository: PlaybackControlRepository,
) {
    operator fun invoke(): Flow<PlaybackState> = repository.observePlaybackState()
}

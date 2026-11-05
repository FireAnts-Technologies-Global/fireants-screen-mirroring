package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.PlaybackState
import com.anrstudio.template.domain.repository.PlaybackControlRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePlaybackStateUseCase @Inject constructor(
    private val repository: PlaybackControlRepository,
) {
    operator fun invoke(): Flow<PlaybackState> = repository.observePlaybackState()
}

package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.model.MediaItem
import com.hyper.castwave.domain.repository.CastSessionRepository
import javax.inject.Inject

class CastMediaUseCase @Inject constructor(
    private val repository: CastSessionRepository,
) {
    suspend operator fun invoke(item: MediaItem): DataResult<Unit> = repository.castMedia(item)
}

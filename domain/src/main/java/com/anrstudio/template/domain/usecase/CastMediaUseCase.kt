package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.model.MediaItem
import com.anrstudio.template.domain.repository.CastSessionRepository
import javax.inject.Inject

class CastMediaUseCase @Inject constructor(
    private val repository: CastSessionRepository,
) {
    suspend operator fun invoke(item: MediaItem): DataResult<Unit> = repository.castMedia(item)
}

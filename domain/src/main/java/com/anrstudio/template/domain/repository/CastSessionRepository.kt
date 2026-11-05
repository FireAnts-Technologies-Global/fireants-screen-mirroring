package com.anrstudio.template.domain.repository

import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.model.MediaItem

interface CastSessionRepository {
    suspend fun castMedia(item: MediaItem): DataResult<Unit>
    suspend fun stopCasting()
}

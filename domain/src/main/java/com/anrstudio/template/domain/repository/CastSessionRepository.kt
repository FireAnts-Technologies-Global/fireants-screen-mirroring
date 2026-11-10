package com.hyper.castwave.domain.repository

import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.model.MediaItem

interface CastSessionRepository {
    suspend fun castMedia(item: MediaItem): DataResult<Unit>
    suspend fun stopCasting()
}

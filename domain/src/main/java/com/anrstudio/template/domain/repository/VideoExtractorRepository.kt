package com.hyper.castwave.domain.repository

import com.hyper.castwave.domain.model.ExtractedVideo

interface VideoExtractorRepository {
    suspend fun extractVideos(url: String): List<ExtractedVideo>
}

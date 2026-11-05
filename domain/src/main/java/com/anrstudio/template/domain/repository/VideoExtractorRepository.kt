package com.anrstudio.template.domain.repository

import com.anrstudio.template.domain.model.ExtractedVideo

interface VideoExtractorRepository {
    suspend fun extractVideos(url: String): List<ExtractedVideo>
}

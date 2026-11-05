package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.ExtractedVideo
import com.anrstudio.template.domain.repository.VideoExtractorRepository
import javax.inject.Inject

class ExtractVideosUseCase @Inject constructor(
    private val repository: VideoExtractorRepository,
) {
    suspend operator fun invoke(url: String): List<ExtractedVideo> = repository.extractVideos(url)
}

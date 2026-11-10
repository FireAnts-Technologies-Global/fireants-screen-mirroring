package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.ExtractedVideo
import com.hyper.castwave.domain.repository.VideoExtractorRepository
import javax.inject.Inject

class ExtractVideosUseCase @Inject constructor(
    private val repository: VideoExtractorRepository,
) {
    suspend operator fun invoke(url: String): List<ExtractedVideo> = repository.extractVideos(url)
}

package com.anrstudio.template.data.repository

import android.content.Context
import com.anrstudio.template.data.videoextractor.UniversalVideoExtractor
import com.anrstudio.template.domain.model.ExtractedVideo
import com.anrstudio.template.domain.repository.VideoExtractorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class VideoExtractorRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : VideoExtractorRepository {

    override suspend fun extractVideos(url: String): List<ExtractedVideo> {
        return suspendCancellableCoroutine { continuation ->
            val extractor = UniversalVideoExtractor(context)
            extractor.extractVideo(url, object : UniversalVideoExtractor.VideoExtractionCallback {
                override fun onVideoFound(video: ExtractedVideo) {}

                override fun onExtractionComplete(videos: List<ExtractedVideo>) {
                    continuation.resume(videos)
                }

                override fun onError(error: String) {
                    continuation.resume(emptyList())
                }

                override fun onProgress(message: String) {}
            })
        }
    }
}

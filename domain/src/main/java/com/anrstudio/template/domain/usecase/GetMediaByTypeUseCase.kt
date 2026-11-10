package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.model.MediaItem
import com.hyper.castwave.domain.model.MediaTab
import com.hyper.castwave.domain.repository.MediaRepository
import javax.inject.Inject

class GetMediaByTypeUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    suspend operator fun invoke(tab: MediaTab): DataResult<List<MediaItem>> = when (tab) {
        MediaTab.PHOTOS -> repository.getPhotos()
        MediaTab.VIDEOS -> repository.getVideos()
        MediaTab.AUDIO -> repository.getAudio()
        MediaTab.RECENTLY -> repository.getRecently()
        MediaTab.FAVORITE -> repository.getFavorites()
        MediaTab.PORTRAIT -> repository.getPortrait()
    }
}

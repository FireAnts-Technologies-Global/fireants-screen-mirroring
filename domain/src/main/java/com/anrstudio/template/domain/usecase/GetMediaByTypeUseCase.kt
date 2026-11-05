package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.model.MediaItem
import com.anrstudio.template.domain.model.MediaTab
import com.anrstudio.template.domain.repository.MediaRepository
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

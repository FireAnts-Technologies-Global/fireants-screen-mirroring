package com.hyper.castwave.domain.repository

import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.model.MediaItem

interface MediaRepository {
    suspend fun getPhotos(): DataResult<List<MediaItem>>
    suspend fun getVideos(): DataResult<List<MediaItem>>
    suspend fun getAudio(): DataResult<List<MediaItem>>
    suspend fun getRecently(): DataResult<List<MediaItem>>
    suspend fun getFavorites(): DataResult<List<MediaItem>>
    suspend fun getPortrait(): DataResult<List<MediaItem>>
    suspend fun toggleFavorite(id: String)
}

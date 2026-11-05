package com.anrstudio.template.domain.model

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val mediaInfo: MediaInfo? = null,
)

data class MediaInfo(
    val title: String,
    val thumbnailUrl: String?,
    val contentType: String,
)

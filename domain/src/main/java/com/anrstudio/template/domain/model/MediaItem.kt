package com.anrstudio.template.domain.model

data class MediaItem(
    val id: String,
    val name: String,
    val uri: String,
    val thumbnailUri: String,
    val type: MediaType,
    val duration: Long?,
    val dateAdded: Long,
    val isFavorite: Boolean,
)

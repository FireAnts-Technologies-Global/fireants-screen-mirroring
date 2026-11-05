package com.anrstudio.template.domain.model

data class ExtractedVideo(
    val name: String,
    val thumbnail: String,
    val url: String,
    val quality: String,
    val format: String,
    val duration: Long,
)

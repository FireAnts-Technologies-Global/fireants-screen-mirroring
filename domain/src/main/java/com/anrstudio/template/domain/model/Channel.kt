package com.hyper.castwave.domain.model

data class Channel(
    val id: String,
    val name: String,
    val iconRes: Int,
    val url: String,
    val type: ChannelType,
)

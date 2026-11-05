package com.anrstudio.template.domain.model

data class CastDevice(
    val id: String,
    val name: String,
    val modelName: String,
    val isConnected: Boolean,
    val type: DeviceType,
)

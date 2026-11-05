package com.anrstudio.template.data.repository

import com.connectsdk.device.ConnectableDevice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartTvSessionStore @Inject constructor() {
    var selectedDevice: SmartTvDevice? = null
}

data class SmartTvDevice(
    val id: String,
    val name: String,
    val modelName: String,
    val device: ConnectableDevice,
)

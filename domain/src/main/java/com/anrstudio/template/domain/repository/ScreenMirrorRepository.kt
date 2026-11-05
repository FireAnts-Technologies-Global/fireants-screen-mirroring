package com.anrstudio.template.domain.repository

import kotlinx.coroutines.flow.Flow

interface ScreenMirrorRepository {

    val isMirroring: Flow<Boolean>

    val serverUrl: Flow<String>

    fun start(resultCode: Int, projectionData: Any)

    fun stop()
}

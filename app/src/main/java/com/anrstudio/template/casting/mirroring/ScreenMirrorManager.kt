package com.anrstudio.template.casting.mirroring

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.anrstudio.template.data.screenmirror.HlsStore
import com.anrstudio.template.data.server.UnifiedMediaServer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenMirrorManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hlsStore: HlsStore,
    private val unifiedMediaServer: UnifiedMediaServer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _isMirroring = MutableStateFlow(false)
    val isMirroring: StateFlow<Boolean> = _isMirroring.asStateFlow()

    private val _segmentsCommitted = MutableStateFlow(0)
    val segmentsCommitted: StateFlow<Int> = _segmentsCommitted.asStateFlow()

    private var mirrorJob: Job? = null

    fun start(resultCode: Int, data: Intent) {
        mirrorJob?.cancel()
        hlsStore.clear()
        _segmentsCommitted.value = 0

        val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data) ?: return

        _isMirroring.value = true

        mirrorJob = scope.launch {
            try {
                ScreenMirrorEngine(
                    context = context,
                    mediaProjection = mediaProjection,
                    hlsStore = hlsStore,
                    onSegmentCommitted = { seg ->
                        _segmentsCommitted.value = seg + 1
                    },
                ).run()

            } catch (e: CancellationException) {
                e.printStackTrace()
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isMirroring.value = false
            }
        }
    }

    fun stop() {
        mirrorJob?.cancel()
        mirrorJob = null
        hlsStore.clear()
        _isMirroring.value = false
        _segmentsCommitted.value = 0
    }

    fun hlsUrl(): String? = unifiedMediaServer.hlsPlaylistUrl()


}

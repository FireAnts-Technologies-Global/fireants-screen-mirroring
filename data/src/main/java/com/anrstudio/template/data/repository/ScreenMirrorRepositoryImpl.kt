package com.hyper.castwave.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.hyper.castwave.data.screenmirror.MjpegServer
import com.hyper.castwave.data.screenmirror.NetworkUtils
import com.hyper.castwave.data.screenmirror.ScreenRecorder
import com.hyper.castwave.domain.repository.ScreenMirrorRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenMirrorRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ScreenMirrorRepository {

    private val _isMirroring = MutableStateFlow(false)
    override val isMirroring: StateFlow<Boolean> = _isMirroring.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    override val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val mjpegServer = MjpegServer()
    private var serverThread: Thread? = null

    private var binder: ScreenRecorder.RecordingBinder? = null
    private var pendingProjection: Pair<Int, Intent>? = null
    private var isBound = false

    private val callback = object : ScreenRecorder.RecordingCallback {
        override fun onRecordingStart() {
            _isMirroring.value = true
        }

        override fun onRecordingStop() {
            _isMirroring.value = false
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val recordingBinder = service as ScreenRecorder.RecordingBinder
            binder = recordingBinder
            recordingBinder.setConnect(callback)
            _isMirroring.value = recordingBinder.isStarted
            pendingProjection?.let { (code, data) ->
                pendingProjection = null
                launchMirror(code, data)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            binder = null
        }
    }

    init {
        ensureServerStarted()
        bindRecorderService()
    }

    override fun start(resultCode: Int, projectionData: Any) {
        val data = projectionData as Intent
        val current = binder
        if (current == null) {
            pendingProjection = resultCode to data
            return
        }
        launchMirror(resultCode, data)
    }

    override fun stop() {
        binder?.stopService()
    }

    private fun launchMirror(resultCode: Int, data: Intent) {
        val current = binder ?: return
        if (current.isStarted) return
        current.setPreStart(resultCode, data)
        current.setStart()
        ContextCompat.startForegroundService(
            context,
            Intent(context, ScreenRecorder::class.java),
        )
    }

    private fun ensureServerStarted() {
        val ip = NetworkUtils.getWifiIpAddress(context).orEmpty()
        _serverUrl.value = "http://$ip:${mjpegServer.port}"
        if (serverThread == null) {
            serverThread = Thread(mjpegServer).also { it.start() }
        }
    }

    private fun bindRecorderService() {
        if (isBound) return
        val intent = Intent(context, ScreenRecorder::class.java)
        isBound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
}

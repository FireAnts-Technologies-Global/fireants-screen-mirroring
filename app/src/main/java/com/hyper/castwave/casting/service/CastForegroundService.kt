package com.hyper.castwave.casting.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.hyper.castwave.R
import com.hyper.castwave.casting.mirroring.ScreenMirrorManager
import com.hyper.castwave.domain.model.MediaItem
import com.hyper.castwave.domain.model.MediaType
import com.hyper.castwave.domain.repository.CastSessionRepository
import com.hyper.castwave.domain.usecase.DisconnectDeviceUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class CastForegroundService : Service() {

    @Inject lateinit var disconnectDeviceUseCase: DisconnectDeviceUseCase
    @Inject lateinit var screenMirrorManager: ScreenMirrorManager
    @Inject lateinit var castSessionRepository: CastSessionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var lastDeviceName = "TV"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch { disconnectDeviceUseCase() }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                START_NOT_STICKY
            }
            ACTION_START_MIRRORING -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = intentExtra(intent) ?: run {
                    return START_NOT_STICKY
                }
                showForegroundNotification(lastDeviceName, isMirroring = true)
                serviceScope.launch { runMirroring(resultCode, data) }
                START_NOT_STICKY
            }
            else -> {
                intent?.getStringExtra(EXTRA_DEVICE_NAME)?.let { lastDeviceName = it }
                showForegroundNotification(lastDeviceName, isMirroring = false)
                START_NOT_STICKY
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        screenMirrorManager.stop()
        serviceScope.cancel()
    }

    private suspend fun runMirroring(resultCode: Int, data: Intent) {
        screenMirrorManager.start(resultCode, data)

        val ready = withTimeoutOrNull(SEGMENT_WAIT_TIMEOUT_MS) {
            screenMirrorManager.segmentsCommitted.first { it >= MIN_SEGMENTS }
        }
        if (ready == null) {
            return
        }
        if (!screenMirrorManager.isMirroring.value) {
            return
        }
        val hlsUrl = screenMirrorManager.hlsUrl()
        if (hlsUrl == null) return

        val result = castSessionRepository.castMedia(
            MediaItem(
                id = "mirror",
                name = "Screen Mirror",
                uri = hlsUrl,
                thumbnailUri = "",
                type = MediaType.VIDEO,
                duration = null,
                dateAdded = 0,
                isFavorite = false,
            )
        )
    }

    private fun showForegroundNotification(deviceName: String, isMirroring: Boolean) {
        val notification = buildNotification(deviceName)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            if (isMirroring) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            type
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun buildNotification(deviceName: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.cast_ic_cast)
        .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
        .setContentTitle(getString(R.string.cast_notification_casting_title, deviceName))
        .setContentText(getString(R.string.cast_notification_casting_content))
        .setContentIntent(openAppPendingIntent())
        .addAction(0, getString(R.string.cast_notification_stop_casting), stopPendingIntent())
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .build()

    private fun openAppPendingIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        packageManager.getLaunchIntentForPackage(packageName),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun stopPendingIntent(): PendingIntent = PendingIntent.getService(
        this, 0,
        Intent(this, CastForegroundService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.cast_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = getString(R.string.cast_notification_channel_desc)
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    @Suppress("DEPRECATION")
    private fun intentExtra(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }

    companion object {
        const val ACTION_STOP = "com.hyper.castwave.cast.STOP_CASTING"
        const val ACTION_START_MIRRORING = "com.hyper.castwave.cast.START_MIRRORING"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val MIN_SEGMENTS = 3
        private const val SEGMENT_WAIT_TIMEOUT_MS = 30_000L
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "cast_session_channel"
    }
}

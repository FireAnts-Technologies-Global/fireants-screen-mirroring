package com.anrstudio.template.data.repository

import android.content.Context
import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.model.PlaybackState
import com.anrstudio.template.domain.repository.PlaybackControlRepository
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlaybackControlRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlaybackControlRepository {

    override fun observePlaybackState(): Flow<PlaybackState> = callbackFlow {
        val listener = object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                val client = getRemoteMediaClient()
                if (client != null) {
                    trySend(buildPlaybackState(client))
                }
            }

            override fun onMetadataUpdated() {
                val client = getRemoteMediaClient()
                if (client != null) {
                    trySend(buildPlaybackState(client))
                }
            }
        }

        val client = getRemoteMediaClient()
        if (client != null) {
            client.registerCallback(listener)
            trySend(buildPlaybackState(client))
        } else {
            trySend(PlaybackState())
        }

        val pollJob = launch {
            while (isActive) {
                delay(1000L)
                val currentClient = getRemoteMediaClient()
                if (currentClient != null &&
                    currentClient.mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING
                ) {
                    trySend(buildPlaybackState(currentClient))
                }
            }
        }

        awaitClose {
            pollJob.cancel()
            client?.unregisterCallback(listener)
        }
    }

    override suspend fun play(): DataResult<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val client = getRemoteMediaClient()
                ?: return@withContext DataResult.Error(
                    IllegalStateException("No remote media client"),
                    "Not connected to device"
                )

            if (isMediaFinished(client)) {
                return@withContext reloadAndPlay(client, 0L)
            }

            suspendCancellableCoroutine { cont ->
                client.play().setResultCallback { result ->
                    if (result.status.isSuccess) {
                        cont.resume(DataResult.Success(Unit))
                    } else {
                        cont.resume(
                            DataResult.Error(
                                RuntimeException("Play failed"),
                                "Failed to play"
                            )
                        )
                    }
                }
            }
        }.getOrElse {
            DataResult.Error(it, it.message ?: "Play failed")
        }
    }

    override suspend fun pause(): DataResult<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val client = getRemoteMediaClient()
                ?: return@withContext DataResult.Error(
                    IllegalStateException("No remote media client"),
                    "Not connected to device"
                )

            suspendCancellableCoroutine { cont ->
                client.pause().setResultCallback { result ->
                    if (result.status.isSuccess) {
                        cont.resume(DataResult.Success(Unit))
                    } else {
                        cont.resume(
                            DataResult.Error(
                                RuntimeException("Pause failed"),
                                "Failed to pause"
                            )
                        )
                    }
                }
            }
        }.getOrElse {
            DataResult.Error(it, it.message ?: "Pause failed")
        }
    }

    override suspend fun seek(position: Long): DataResult<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val client = getRemoteMediaClient()
                ?: return@withContext DataResult.Error(
                    IllegalStateException("No remote media client"),
                    "Not connected to device"
                )

            if (isMediaFinished(client)) {
                return@withContext reloadAndPlay(client, position)
            }

            suspendCancellableCoroutine { cont ->
                client.seek(position).setResultCallback { result ->
                    if (result.status.isSuccess) {
                        cont.resume(DataResult.Success(Unit))
                    } else {
                        cont.resume(
                            DataResult.Error(
                                RuntimeException("Seek failed"),
                                "Failed to seek"
                            )
                        )
                    }
                }
            }
        }.getOrElse {
            DataResult.Error(it, it.message ?: "Seek failed")
        }
    }

    override suspend fun setVolume(volume: Float): DataResult<Unit> =
        withContext(Dispatchers.Main) {
            runCatching {
                val session = CastContext.getSharedInstance(context)
                    .sessionManager.currentCastSession
                    ?: return@withContext DataResult.Error(
                        IllegalStateException("No active cast session"),
                        "Not connected to device"
                    )

                session.volume = volume.toDouble().coerceIn(0.0, 1.0)
                DataResult.Success(Unit)
            }.getOrElse {
                DataResult.Error(it, it.message ?: "Volume change failed")
            }
        }

    override suspend fun stop(): DataResult<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val client = getRemoteMediaClient()
                ?: return@withContext DataResult.Error(
                    IllegalStateException("No remote media client"),
                    "Not connected to device"
                )

            suspendCancellableCoroutine { cont ->
                client.stop().setResultCallback { result ->
                    if (result.status.isSuccess) {
                        cont.resume(DataResult.Success(Unit))
                    } else {
                        cont.resume(
                            DataResult.Error(
                                RuntimeException("Stop failed"),
                                "Failed to stop"
                            )
                        )
                    }
                }
            }
        }.getOrElse {
            DataResult.Error(it, it.message ?: "Stop failed")
        }
    }

    private fun getRemoteMediaClient(): RemoteMediaClient? {
        return try {
            CastContext.getSharedInstance(context)
                .sessionManager
                .currentCastSession
                ?.remoteMediaClient
        } catch (e: Exception) {
            null
        }
    }

    private fun buildPlaybackState(client: RemoteMediaClient): PlaybackState {
        val mediaStatus = client.mediaStatus
        val mediaInfo = client.mediaInfo

        return PlaybackState(
            isPlaying = mediaStatus?.playerState == MediaStatus.PLAYER_STATE_PLAYING,
            currentPosition = client.approximateStreamPosition,
            duration = mediaInfo?.streamDuration ?: 0L,
            mediaInfo = mediaInfo?.let {
                com.anrstudio.template.domain.model.MediaInfo(
                    title = it.metadata?.getString(com.google.android.gms.cast.MediaMetadata.KEY_TITLE)
                        ?: "Unknown",
                    thumbnailUrl = it.metadata?.images?.firstOrNull()?.url?.toString(),
                    contentType = it.contentType ?: "video/mp4"
                )
            }
        )
    }

    private fun isMediaFinished(client: RemoteMediaClient): Boolean {
        val status = client.mediaStatus ?: return false
        return status.playerState == MediaStatus.PLAYER_STATE_IDLE
                && status.idleReason == MediaStatus.IDLE_REASON_FINISHED
    }

    private suspend fun reloadAndPlay(
        client: RemoteMediaClient,
        startPosition: Long
    ): DataResult<Unit> {
        val mediaInfo = client.mediaInfo
            ?: return DataResult.Error(
                RuntimeException("No media info"),
                "No media to play"
            )

        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .setCurrentTime(startPosition)
            .build()

        return suspendCancellableCoroutine { cont ->
            client.load(request).setResultCallback { result ->
                if (result.status.isSuccess) {
                    cont.resume(DataResult.Success(Unit))
                } else {
                    cont.resume(
                        DataResult.Error(
                            RuntimeException("Reload failed"),
                            "Failed to play"
                        )
                    )
                }
            }
        }
    }
}

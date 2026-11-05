package com.anrstudio.template.data.repository

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import com.anrstudio.template.data.server.UnifiedMediaServer
import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.model.MediaItem
import com.anrstudio.template.domain.model.MediaType
import com.anrstudio.template.domain.repository.CastSessionRepository
import com.connectsdk.service.DLNAService
import com.connectsdk.service.capability.MediaPlayer
import com.connectsdk.service.command.ServiceCommandError
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CastSessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val unifiedMediaServer: UnifiedMediaServer,
    private val smartTvSessionStore: SmartTvSessionStore,
) : CastSessionRepository {


    override suspend fun castMedia(item: MediaItem): DataResult<Unit> =
        withContext(Dispatchers.Main) {
            runCatching {
                Log.d(
                    TAG,
                    "[CastSession] castMedia item.uri=${item.uri} type=${item.type} name=${item.name}"
                )

                val session = CastContext.getSharedInstance(context)
                    .sessionManager.currentCastSession

                if (session == null) {
                    val smartTvDevice = smartTvSessionStore.selectedDevice
                    if (smartTvDevice != null) {
                        return@withContext castSmartTvMedia(smartTvDevice, item)
                    }
                    Log.w(TAG, "[CastSession] castMedia failed: no active cast session")
                    return@withContext DataResult.Error(
                        IllegalStateException("No active cast session"),
                        "Not connected to any device",
                    )
                }

                val client = session.remoteMediaClient
                if (client == null) {
                    Log.w(TAG, "[CastSession] castMedia failed: no remote media client")
                    return@withContext DataResult.Error(
                        IllegalStateException("No remote media client"),
                        "Cast session error",
                    )
                }

                if (item.type == MediaType.VIDEO) {
                    val codec = detectVideoCodec(item.uri)
                    Log.d(TAG, "[CastSession] video codec=$codec")
                }

                val castUrl = resolveUrl(item)
                if (castUrl == null) {
                    Log.w(TAG, "[CastSession] castMedia failed: resolveUrl returned null")
                    return@withContext DataResult.Error(
                        IllegalStateException("Cannot resolve media URL"),
                        "Cannot serve local file — ensure device is on Wi-Fi",
                    )
                }

                Log.d(TAG, "[CastSession] resolved castUrl=$castUrl")

                val mediaInfo = if (castUrl.contains(".m3u8")) {
                    buildHlsMediaInfo(castUrl)
                } else {
                    buildFileMediaInfo(castUrl, item)
                }

                Log.d(
                    TAG,
                    "[CastSession] mediaInfo contentType=${mediaInfo.contentType} streamType=${mediaInfo.streamType}"
                )

                val request = MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaInfo)
                    .setAutoplay(true)
                    .build()

                suspendCancellableCoroutine { cont ->
                    client.load(request).setResultCallback { result ->
                        val statusCode = result.status.statusCode
                        val statusMessage = result.status.statusMessage
                        val isSuccess = result.status.isSuccess

                        Log.d(
                            TAG,
                            "[CastSession] LOAD result: success=$isSuccess statusCode=$statusCode statusMessage=$statusMessage"
                        )

                        if (isSuccess) {
                            cont.resume(DataResult.Success(Unit))
                        } else {

                            val errorMessage = when {
                                statusCode == 2001 -> "Video URL không hợp lệ hoặc đã hết hạn. Vui lòng thử lại."
                                statusCode == 2003 -> "TV không hỗ trợ định dạng video này."
                                statusCode == 2100 -> "Không thể tải video. Kiểm tra kết nối mạng của TV."
                                statusMessage?.contains("LOAD_FAILED", ignoreCase = true) == true ->
                                    "Không thể tải video. URL có thể đã hết hạn hoặc TV không truy cập được."

                                statusMessage?.contains("Invalid", ignoreCase = true) == true ->
                                    "URL video không hợp lệ."

                                else -> statusMessage ?: "Cast failed (code: $statusCode)"
                            }

                            Log.e(TAG, "[CastSession] LOAD failed: $errorMessage")
                            cont.resume(
                                DataResult.Error(
                                    RuntimeException("Cast failed: $errorMessage"),
                                    errorMessage,
                                )
                            )
                        }
                    }
                }
            }.getOrElse {
                DataResult.Error(it, it.message ?: "Cast failed")
            }
        }

    override suspend fun stopCasting() = withContext(Dispatchers.Main) {
        runCatching {
            CastContext.getSharedInstance(context).sessionManager.endCurrentSession(true)
        }
        Unit
    }

    private suspend fun detectVideoCodec(uri: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, Uri.parse(uri), null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/")) {
                    extractor.release()
                    return@withContext mime
                }
            }
            extractor.release()
            "unknown"
        }.getOrDefault("unknown")
    }

    private fun resolveUrl(item: MediaItem): String? {

        val uri = item.uri

        Log.d(
            TAG,
            "[CastSession] resolveUrl input=$uri"
        )

        if (
            uri.startsWith("http://") ||
            uri.startsWith("https://")
        ) {

            val lower =
                uri.lowercase()

            when {

                lower.contains(".m3u8") -> {

                    Log.d(
                        TAG,
                        "[CastSession] detected HLS stream"
                    )

                    return uri
                }

                lower.contains("googlevideo.com") -> {

                    Log.d(
                        TAG,
                        "[CastSession] detected googlevideo direct"
                    )

                    return uri
                }

                lower.contains(".mp4") ||
                        lower.contains(".webm") ||
                        lower.contains(".mkv") -> {

                    Log.d(
                        TAG,
                        "[CastSession] detected direct video"
                    )

                    return uri
                }

                lower.contains("twitch.tv") -> {

                    Log.e(
                        TAG,
                        "[CastSession] twitch page url unsupported"
                    )

                    return null
                }

                else -> {

                    Log.d(
                        TAG,
                        "[CastSession] proxy remote url"
                    )

                    return unifiedMediaServer
                        .registerRemoteUrl(uri)
                }
            }
        }

        val localUrl =
            unifiedMediaServer.registerLocalUrl(
                Uri.parse(uri)
            )

        Log.d(
            TAG,
            "[CastSession] resolveUrl local -> url=$localUrl"
        )

        return localUrl
    }

    private suspend fun castSmartTvMedia(device: SmartTvDevice, item: MediaItem): DataResult<Unit> {
        unifiedMediaServer.start()
        val castUrl = resolveUrl(item)
            ?: return DataResult.Error(
                IllegalStateException("Cannot resolve media URL"),
                "Cannot serve local file — ensure device is on Wi-Fi",
            )
        val services = device.device.services.joinToString { service ->
            "${service.serviceName}:${service::class.java.simpleName}"
        }
        Log.d(TAG, "[SmartTV] services=$services")
        val mediaPlayer =
            device.device.dlnaMediaPlayer() ?: device.device.getCapability(MediaPlayer::class.java)
            ?: return DataResult.Error(
                IllegalStateException("MediaPlayer capability not supported"),
                "TV does not support media playback",
            )
        val mediaInfo =
            com.connectsdk.core.MediaInfo.Builder(castUrl, resolveMimeTypeFromUrl(castUrl, item))
                .setTitle(item.name.ifEmpty { "Video" })
                .setDescription(item.name.ifEmpty { "Video" })
                .build()
        Log.d(
            TAG,
            "[SmartTV] playMedia device=${device.name} player=${mediaPlayer::class.java.simpleName} url=$castUrl mime=${mediaInfo.mimeType}"
        )
        return withTimeoutOrNull(SMART_TV_PLAY_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                mediaPlayer.playMedia(mediaInfo, false, object : MediaPlayer.LaunchListener {
                    override fun onSuccess(mediaLaunchObject: MediaPlayer.MediaLaunchObject) {
                        Log.d(TAG, "[SmartTV] playMedia success")
                        continuation.resume(DataResult.Success(Unit))
                    }

                    override fun onError(error: ServiceCommandError) {
                        Log.e(
                            TAG,
                            "[SmartTV] playMedia failed message=${error.message} code=${error.code}"
                        )
                        continuation.resume(
                            DataResult.Error(
                                RuntimeException(error.message),
                                error.message ?: "Smart TV cast failed",
                            )
                        )
                    }
                })
            }
        } ?: DataResult.Error(
            RuntimeException("Smart TV cast timeout"),
            "Smart TV cast timeout",
        )
    }

    private fun com.connectsdk.device.ConnectableDevice.dlnaMediaPlayer(): MediaPlayer? {
        val dlnaService = getServiceByName(DLNAService.ID) ?: return null
        return dlnaService.getAPI(MediaPlayer::class.java)
    }

    private fun buildHlsMediaInfo(url: String): MediaInfo {
        return MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType("application/x-mpegURL")
            .setMetadata(MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE))
            .build()
    }

    private fun buildFileMediaInfo(url: String, item: MediaItem): MediaInfo {
        val mimeType = resolveMimeTypeFromUrl(url, item)

        val metadata = MediaMetadata(item.type.toMetadataType()).apply {
            putString(MediaMetadata.KEY_TITLE, item.name.ifEmpty { "Video" })
            if (item.thumbnailUri.isNotEmpty()) {
                addImage(com.google.android.gms.common.images.WebImage(Uri.parse(item.thumbnailUri)))
            }
        }

        return MediaInfo.Builder(url)
            .setStreamType(
                resolveStreamType(
                    url,
                    item
                )
            )
            .setContentType(mimeType)
            .setMetadata(metadata)
            .build()
    }

    private fun resolveMimeType(item: MediaItem): String {
        if (!item.uri.startsWith("content://")) {
            val defaultMime = item.type.toDefaultMimeType()
            return defaultMime
        }
        val resolvedMime = context.contentResolver.getType(Uri.parse(item.uri))
            ?: item.type.toDefaultMimeType()
        return resolvedMime
    }

    private fun resolveMimeTypeFromUrl(url: String, item: MediaItem): String {
        if (item.uri.startsWith("content://")) {
            val resolvedMime = context.contentResolver.getType(Uri.parse(item.uri))
            if (resolvedMime != null) {
                return resolvedMime
            }
        }

        val mimeType = when {
            url.contains(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
            url.contains(".mpd", ignoreCase = true) -> "application/dash+xml"
            url.contains(".webm", ignoreCase = true) -> "video/webm"
            url.contains(".mp4", ignoreCase = true) -> "video/mp4"
            url.contains(".mkv", ignoreCase = true) -> "video/x-matroska"
            url.contains(".avi", ignoreCase = true) -> "video/x-msvideo"
            url.contains(".mov", ignoreCase = true) -> "video/quicktime"
            url.contains(".flv", ignoreCase = true) -> "video/x-flv"
            url.contains(".ts", ignoreCase = true) -> "video/mp2t"
            url.contains("googlevideo.com") -> "video/mp4"
            else -> item.type.toDefaultMimeType()
        }

        return mimeType
    }

    private fun MediaType.toMetadataType() = when (this) {
        MediaType.VIDEO -> MediaMetadata.MEDIA_TYPE_MOVIE
        MediaType.IMAGE -> MediaMetadata.MEDIA_TYPE_PHOTO
        MediaType.AUDIO -> MediaMetadata.MEDIA_TYPE_MUSIC_TRACK
    }

    private fun MediaType.toStreamType() = when (this) {
        MediaType.IMAGE -> MediaInfo.STREAM_TYPE_NONE
        else -> MediaInfo.STREAM_TYPE_BUFFERED
    }

    private fun MediaType.toDefaultMimeType() = when (this) {
        MediaType.VIDEO -> "video/mp4"
        MediaType.IMAGE -> "image/jpeg"
        MediaType.AUDIO -> "audio/mpeg"
    }

    private fun resolveStreamType(
        url: String,
        item: MediaItem
    ): Int {

        val lower =
            url.lowercase()

        val isLive =
            lower.contains(".m3u8") ||
                    lower.contains("twitch") ||
                    item.name.contains("live", true)

        return when {

            item.type == MediaType.IMAGE ->
                MediaInfo.STREAM_TYPE_NONE

            isLive ->
                MediaInfo.STREAM_TYPE_LIVE

            else ->
                MediaInfo.STREAM_TYPE_BUFFERED
        }
    }

    private companion object {
        const val TAG = "cast_debug_android"
        const val SMART_TV_PLAY_TIMEOUT_MS = 12000L
    }

}



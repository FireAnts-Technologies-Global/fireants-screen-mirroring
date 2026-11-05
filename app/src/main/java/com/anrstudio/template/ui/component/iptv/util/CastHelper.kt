package com.anrstudio.template.ui.component.iptv.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.anrstudio.template.R
import com.anrstudio.template.utils.CastManager
import timber.log.Timber

@UnstableApi
class CastHelper(
    private val context: Context,
    private var playerView: PlayerView,
    private var mediaRouteButton: MediaRouteButton? = null,
) {
    companion object {
        private const val TAG = "CastHelper"
        private const val CAST_SESSION_READY_DELAY_MS = 500L
    }

    private var castManager: CastManager? = null
    private var castPlayer: CastPlayer? = null
    private var exoPlayer: ExoPlayer? = null
    private var currentPlayer: Player? = null
    private var currentMediaItem: MediaItem? = null
    private var castSessionListener: SessionManagerListener<CastSession>? = null

    var onPlayerChanged: ((Player) -> Unit)? = null
    var onCastStarted: (() -> Unit)? = null
    var onCastEnded: (() -> Unit)? = null
    var onPlaybackStateChanged: ((Int, String) -> Unit)? = null
    var onPlayerError: ((String) -> Unit)? = null
    var onCastLoading: ((Boolean) -> Unit)? = null
    var isInPictureInPictureMode: Boolean = false

    fun initialize() {
        try {
            castManager = CastManager(context)
            castManager?.initialize()
            castPlayer = castManager?.castPlayer

            mediaRouteButton?.let { button ->
                try {
                    CastButtonFactory.setUpMediaRouteButton(context, button)
                    Timber.tag(TAG).d("Cast button initialized successfully")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to setup Cast button: ${e.message}")
                    button.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize Cast: ${e.message}")
            mediaRouteButton?.visibility = View.GONE
        }
    }

    fun setupPlayer(exoPlayer: ExoPlayer, mediaItem: MediaItem) {
        this.exoPlayer = exoPlayer
        this.currentMediaItem = mediaItem

        if (currentPlayer is CastPlayer && castPlayer != null) {
            Timber.tag(TAG).d("Updating CastPlayer with new MediaItem while casting")
            onCastLoading?.invoke(true)
            try {
                castPlayer?.setMediaItem(mediaItem)
                castPlayer?.prepare()
                castPlayer?.playWhenReady = true
                Timber.tag(TAG).d("CastPlayer updated with new MediaItem successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error updating CastPlayer with new MediaItem: ${e.message}")
                onCastLoading?.invoke(false)
                switchToExoPlayer(exoPlayer)
            }
        } else {
            currentPlayer = exoPlayer
            playerView.player = currentPlayer
        }

        setupPlayerListeners(exoPlayer)
        setupCastSessionListener(exoPlayer)
        Timber.tag(TAG).d("Player setup completed, using ${if (currentPlayer is CastPlayer) "CastPlayer" else "ExoPlayer"}")
    }

    private fun setupPlayerListeners(exoPlayer: ExoPlayer) {
        exoPlayer.addListener(createPlayerListener("ExoPlayer", exoPlayer))

        castPlayer?.let {
            it.addListener(createPlayerListener("Cast", it))
        }
    }

    private fun createPlayerListener(type: String, player: Player): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val message = when (playbackState) {
                    Player.STATE_BUFFERING -> context.getString(R.string.cast_downloading_stream)
                    Player.STATE_READY -> if (type == "Cast") context.getString(R.string.cast_cast_successfully) else context.getString(
                        R.string.cast_play_successfully
                    )
                    Player.STATE_ENDED -> context.getString(R.string.cast_finish)
                    else -> null
                }
                message?.let {
                    Timber.tag(TAG).d("$type: $it")
                    if (!isInPictureInPictureMode && currentPlayer == player) {
                        if (type == "Cast" && (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED)) {
                            onCastLoading?.invoke(false)
                        }
                        onPlaybackStateChanged?.invoke(playbackState, it)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.tag(TAG).e(error, "$type lỗi phát: ${error.message}")
                val msg = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                        context.getString(R.string.cast_network_error)

                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                        context.getString(R.string.cast_playlist_invalid_format)

                    else -> error.message ?: "Không thể phát"
                }
                if (!isInPictureInPictureMode && currentPlayer == player) {
                    if (type == "Cast") {
                        onCastLoading?.invoke(false)
                    }
                    onPlayerError?.invoke(msg)
                }
            }

            override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
                when (deviceInfo.playbackType) {
                    DeviceInfo.PLAYBACK_TYPE_LOCAL -> {
                        Timber.tag(TAG).d("$type: Playback switched to LOCAL")
                    }

                    DeviceInfo.PLAYBACK_TYPE_REMOTE -> {
                        Timber.tag(TAG).d("$type: Playback switched to REMOTE (Cast)")
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Timber.tag(TAG).d("$type: isPlaying changed to $isPlaying")
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Timber.tag(TAG)
                    .d("$type: MediaItem transition - ${mediaItem?.mediaId}, reason: $reason")
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupCastSessionListener(exoPlayer: ExoPlayer) {
        castManager?.let {
            val sessionManager = CastContext.getSharedInstance(context)?.sessionManager

            castSessionListener?.let { oldListener ->
                sessionManager?.removeSessionManagerListener(oldListener, CastSession::class.java)
            }

            castSessionListener = object : SessionManagerListener<CastSession> {
                override fun onSessionStarted(session: CastSession, sessionId: String) {
                    Timber.tag(TAG)
                        .d("Cast session started: $sessionId, isConnected: ${session.isConnected}")
                    Handler(Looper.getMainLooper()).postDelayed({
                        switchToCastPlayer(session, exoPlayer)
                    }, CAST_SESSION_READY_DELAY_MS)
                }

                override fun onSessionEnded(session: CastSession, error: Int) {
                    Timber.tag(TAG).d("Cast session ended, switching to ExoPlayer (error: $error)")
                    Handler(Looper.getMainLooper()).post {
                        switchToExoPlayer(exoPlayer)
                    }
                }

                override fun onSessionStartFailed(session: CastSession, error: Int) {
                    Timber.tag(TAG).e("Cast session start failed: $error")
                    onCastLoading?.invoke(false)
                }

                override fun onSessionStarting(session: CastSession) {
                    Timber.tag(TAG).d("Cast session starting...")
                    onCastLoading?.invoke(true)
                }

                override fun onSessionEnding(session: CastSession) {
                    Timber.tag(TAG).d("Cast session ending...")
                }

                override fun onSessionResuming(session: CastSession, sessionId: String) {
                    Timber.tag(TAG).d("Cast session resuming...")
                }

                override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                    Timber.tag(TAG).d("Cast session resumed")
                }

                override fun onSessionResumeFailed(session: CastSession, error: Int) {
                    Timber.tag(TAG).e("Cast session resume failed: $error")
                }

                override fun onSessionSuspended(session: CastSession, reason: Int) {
                    Timber.tag(TAG).d("Cast session suspended: $reason")
                }
            }

            castSessionListener?.let { listener ->
                sessionManager?.addSessionManagerListener(listener, CastSession::class.java)
            }
        }
    }

    private fun switchToCastPlayer(session: CastSession, exoPlayer: ExoPlayer) {
        try {
            if (castPlayer == null) {
                Timber.tag(TAG).e("CastPlayer is null")
                onCastLoading?.invoke(false)
                return
            }

            if (!session.isConnected) {
                Timber.tag(TAG).e("Cast session is not connected")
                onCastLoading?.invoke(false)
                return
            }

            val remoteMediaClient = session.remoteMediaClient
            if (remoteMediaClient == null) {
                Timber.tag(TAG).e("RemoteMediaClient is null")
                onCastLoading?.invoke(false)
                return
            }

            Timber.tag(TAG).d("Switching to CastPlayer, setting MediaItem...")

            onCastLoading?.invoke(true)

            currentPlayer = castPlayer
            playerView.player = currentPlayer
            onPlayerChanged?.invoke(castPlayer!!)

            currentMediaItem?.let { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                Timber.tag(TAG)
                    .d("Setting MediaItem: URI=$uri, MimeType=${item.localConfiguration?.mimeType}")

                castPlayer?.setMediaItem(item)
                castPlayer?.prepare()
                castPlayer?.playWhenReady = true

                Timber.tag(TAG).d("MediaItem set, prepare() and playWhenReady called")
            } ?: run {
                Timber.tag(TAG).e("currentMediaItem is null")
                onCastLoading?.invoke(false)
            }

            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            }

            onCastStarted?.invoke()
            Timber.tag(TAG).d("Successfully switched to CastPlayer")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error switching to CastPlayer: ${e.message}")
            onCastLoading?.invoke(false)
            onPlayerError?.invoke("Lỗi khi cast: ${e.message}")
        }
    }

    private fun switchToExoPlayer(exoPlayer: ExoPlayer) {
        try {
            onCastLoading?.invoke(false)
            
            currentPlayer = exoPlayer
            playerView.player = currentPlayer
            onPlayerChanged?.invoke(exoPlayer)

            if (exoPlayer.mediaItemCount > 0) {
                exoPlayer.playWhenReady = true
            }

            onCastEnded?.invoke()
            Timber.tag(TAG).d("Successfully switched back to ExoPlayer")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error switching back to ExoPlayer: ${e.message}")
            onCastLoading?.invoke(false)
        }
    }

    fun getCurrentPlayer(): Player? = currentPlayer

    fun isCasting(): Boolean {
        return castManager?.isCasting() == true
    }
    
    fun updatePlayerView(newPlayerView: PlayerView, newMediaRouteButton: MediaRouteButton? = null) {
        playerView = newPlayerView

        newMediaRouteButton?.let { button ->
            mediaRouteButton = button
            try {
                CastButtonFactory.setUpMediaRouteButton(context, button)
                Timber.tag(TAG).d("MediaRouteButton updated successfully")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to setup new MediaRouteButton: ${e.message}")
                button.visibility = View.GONE
            }
        }

        currentPlayer?.let { player ->
            playerView.player = player
        }
    }

    fun onStart() {
        currentPlayer?.let { player ->
            playerView.player = player
        }
    }

    fun onStop() {
        playerView.player = null
    }

    fun release() {
        castSessionListener?.let { listener ->
            try {
                val sessionManager = CastContext.getSharedInstance(context).sessionManager
                sessionManager.removeSessionManagerListener(listener, CastSession::class.java)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error removing Cast session listener: ${e.message}")
            }
        }
        castSessionListener = null

        castManager?.release()
        castManager = null
        castPlayer = null
        exoPlayer = null
        currentPlayer = null
        currentMediaItem = null
    }
}


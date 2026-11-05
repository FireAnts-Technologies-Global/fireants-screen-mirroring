package com.anrstudio.template.ui.component.webbrowser

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.anrstudio.template.ui.bases.BaseViewModel
import com.anrstudio.template.domain.model.CastDevice
import com.anrstudio.template.domain.model.ConnectionState
import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.model.ExtractedVideo
import com.anrstudio.template.domain.model.MediaItem
import com.anrstudio.template.domain.model.MediaType
import com.anrstudio.template.domain.usecase.CastMediaUseCase
import com.anrstudio.template.domain.usecase.ConnectDeviceUseCase
import com.anrstudio.template.domain.usecase.ExtractVideosUseCase
import com.anrstudio.template.domain.usecase.ObserveConnectionStateUseCase
import com.anrstudio.template.domain.usecase.ObserveDevicesUseCase
import com.anrstudio.template.casting.mirroring.ScreenMirrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

@HiltViewModel
class WebBrowserViewModel @Inject constructor(
    private val extractVideos: ExtractVideosUseCase,
    private val castMediaUseCase: CastMediaUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    private val observeDevicesUseCase: ObserveDevicesUseCase,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val screenMirrorManager: ScreenMirrorManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(WebBrowserUiState())
    val uiState: StateFlow<WebBrowserUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WebBrowserEvent>()
    val events: SharedFlow<WebBrowserEvent> = _events.asSharedFlow()

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val connectionState: StateFlow<ConnectionState> =
        observeConnectionStateUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    private val devices: StateFlow<List<CastDevice>> =
        observeDevicesUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var detectionJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                screenMirrorManager.isMirroring,
                connectionState,
                devices,
            ) { isMirroring, conn, devs ->
                Triple(isMirroring, conn, devs)
            }.collect { (isMirroring, conn, devs) ->
                if (!isMirroring) return@collect
                if (conn is ConnectionState.Connected || conn is ConnectionState.Connecting) return@collect
                val target = devs.firstOrNull { it.isConnected } ?: return@collect
                connectDeviceUseCase(target.id)
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateUrl(url: String) {
        _uiState.update { it.copy(currentUrl = url) }
    }

    fun updateProgress(progress: Int) {
        _uiState.update { it.copy(progress = progress, isLoading = progress < 100) }
    }

    fun updateNavState(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { it.copy(canGoBack = canGoBack, canGoForward = canGoForward) }
    }

    fun clearVideoDetection() {
        detectionJob?.cancel()
        _uiState.update { it.copy(hasVideos = false, videos = emptyList()) }
    }

    fun detectVideos(url: String) {
        detectionJob?.cancel()

        if (url.contains("tiktok.com")) {
            val shouldSkip = url.startsWith("snssdk") ||
                url.contains("aweme/detail")
            if (shouldSkip) {
                return
            }
        }

        detectionJob = viewModelScope.launch {
            val videos = extractVideos(url)
            if (videos.isNotEmpty()) {

                _uiState.update { it.copy(hasVideos = true, videos = videos) }
            } else {
                _uiState.update { it.copy(hasVideos = false, videos = emptyList()) }
            }
        }
    }

    fun detectVideosFromWebView(
        webView: android.webkit.WebView,
        onResult: ((List<ExtractedVideo>) -> Unit)? = null,
    ) {
        detectionJob?.cancel()

        val script = """
            (function() {
                var videos = [];
                
                document.querySelectorAll('video').forEach(function(video) {
                    if (video.src || video.currentSrc) {
                        videos.push({url: video.src || video.currentSrc, title: ''});
                    }
                });
                
                document.querySelectorAll('iframe').forEach(function(iframe) {
                    var src = iframe.src;
                    if (src && (src.includes('youtube') || src.includes('vimeo') || src.includes('tiktok'))) {
                        videos.push({url: src, title: ''});
                    }
                });
                
                document.querySelectorAll('a').forEach(function(link) {
                    var href = link.href;
                    if (href && (href.includes('.mp4') || href.includes('.m3u8') || href.includes('.webm'))) {
                        videos.push({url: href, title: link.textContent.trim().substring(0, 100) || ''});
                    }
                });
                
                return JSON.stringify(videos);
            })();
        """.trimIndent()

        webView.evaluateJavascript(script) { result ->
            val videoList = try {
                val rawResult = result ?: "[]"
                val unescaped = rawResult
                    .trim('"')
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                
                val jsonArray = JSONArray(unescaped)
                mutableListOf<ExtractedVideo>().apply {
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val videoUrl = obj.optString("url", "")

                        if (videoUrl.startsWith("blob:")) {
                            continue
                        }
                        
                        if (videoUrl.contains("accounts.google.com") ||
                            videoUrl.contains("ServiceLogin") ||
                            videoUrl.contains("signin") ||
                            videoUrl.contains("login")) {
                            continue
                        }
                        
                        val isVideoUrl = videoUrl.contains(".mp4") || 
                                        videoUrl.contains(".webm") || 
                                        videoUrl.contains(".m3u8") ||
                                        videoUrl.contains(".ogg") ||
                                        videoUrl.contains("googlevideo.com") ||
                                        videoUrl.contains("youtube.com/watch")
                        
                        if (!isVideoUrl) {
                            continue
                        }
                        
                        add(
                            ExtractedVideo(
                                name = obj.optString("title", ""),
                                thumbnail = obj.optString("thumbnail", ""),
                                url = videoUrl,
                                quality = "",
                                format = "",
                                duration = 0L,
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                emptyList<ExtractedVideo>()
            }
            
            if (videoList.isNotEmpty()) {
                _uiState.update { it.copy(hasVideos = true, videos = videoList) }
            }
            onResult?.invoke(videoList)
        }
    }

    fun castVideo(video: ExtractedVideo, isConnected: Boolean) {
        viewModelScope.launch {
            val item = MediaItem(
                id = video.url,
                name = video.name.ifEmpty { video.url },
                uri = video.url,
                thumbnailUri = video.thumbnail,
                type = MediaType.VIDEO,
                duration = video.duration,
                dateAdded = System.currentTimeMillis(),
                isFavorite = false,
            )
            _isCasting.value = true
            try {
                val isMirroring = screenMirrorManager.isMirroring.value
                when {
                    isConnected -> {
                        if (isMirroring) {
                            screenMirrorManager.stop()
                        }
                        castOrNavigate(item)
                    }
                    isMirroring -> {
                        val target = devices.value.firstOrNull { it.isConnected }
                        if (target != null) {
                            connectDeviceUseCase(target.id)
                        }
                        screenMirrorManager.stop()
                        if (target != null) {
                            awaitConnectionAndCast(item)
                        } else {
                            _events.emit(WebBrowserEvent.NavigateToCast)
                        }
                    }
                    else -> {
                        _events.emit(WebBrowserEvent.NavigateToCast)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isCasting.value = false
            }
        }
    }

    private suspend fun castOrNavigate(item: MediaItem) {
        when (val result = castMediaUseCase(item)) {
            is DataResult.Success -> {
                // Cast thành công, mở màn hình điều khiển video
                _events.emit(WebBrowserEvent.NavigateToVideoControl)
                return
            }
            is DataResult.Error -> {
                when {
                    result.message.contains("Not connected", ignoreCase = true) -> {
                        connectAndCast(item)
                    }
                    result.message.contains("Cast session error", ignoreCase = true) -> {
                        var lastError = result
                        repeat(3) { attempt ->
                            delay(150)
                            when (val retry = castMediaUseCase(item)) {
                                is DataResult.Success -> {
                                    _events.emit(WebBrowserEvent.NavigateToVideoControl)
                                    return
                                }
                                is DataResult.Error -> {
                                    lastError = retry
                                }
                            }
                        }
                        _events.emit(WebBrowserEvent.ShowError(lastError.toString()))
                    }
                    else -> {
                        _events.emit(WebBrowserEvent.ShowError(result.message))
                    }
                }
            }
        }
    }

    private suspend fun connectAndCast(item: MediaItem) {
        val target = devices.value.firstOrNull { it.isConnected } ?: run {
            _events.emit(WebBrowserEvent.NavigateToCast)
            return
        }
        when (val result = connectDeviceUseCase(target.id)) {
            is DataResult.Success -> {
                awaitConnectionAndCast(item)
            }
            is DataResult.Error -> {
                _events.emit(WebBrowserEvent.NavigateToCast)
            }
        }
    }

    private suspend fun awaitConnectionAndCast(item: MediaItem) {
        connectionState.first { state ->
            state is ConnectionState.Connected
                || state is ConnectionState.Disconnected
                || state is ConnectionState.PinRequired
        }
        if (connectionState.value is ConnectionState.Connected) {
            castOrNavigate(item)
        } else {
            _events.emit(WebBrowserEvent.NavigateToCast)
        }
    }
}

package com.hyper.castwave.ui.component.media

import androidx.lifecycle.viewModelScope
import com.hyper.castwave.ui.bases.BaseViewModel
import com.hyper.castwave.domain.model.CastDevice
import com.hyper.castwave.domain.model.ConnectionState
import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.model.MediaItem
import com.hyper.castwave.domain.model.MediaTab
import com.hyper.castwave.domain.model.MediaType
import com.hyper.castwave.domain.usecase.CastMediaUseCase
import com.hyper.castwave.domain.usecase.ConnectDeviceUseCase
import com.hyper.castwave.domain.usecase.GetMediaByTypeUseCase
import com.hyper.castwave.domain.usecase.ObserveConnectionStateUseCase
import com.hyper.castwave.domain.usecase.ObserveDevicesUseCase
import com.hyper.castwave.casting.mirroring.ScreenMirrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val getMediaByTypeUseCase: GetMediaByTypeUseCase,
    private val castMediaUseCase: CastMediaUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    private val observeDevicesUseCase: ObserveDevicesUseCase,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val screenMirrorManager: ScreenMirrorManager,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MediaEvent>()
    val events: SharedFlow<MediaEvent> = _events.asSharedFlow()

    private val _castingItemId = MutableStateFlow<String?>(null)
    val castingItemId: StateFlow<String?> = _castingItemId.asStateFlow()

    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()

    private val connectionState: StateFlow<ConnectionState> =
        observeConnectionStateUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    private val devices: StateFlow<List<CastDevice>> =
        observeDevicesUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state !is ConnectionState.Connected) {
                    _castingItemId.value = null
                }
            }
        }
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

    fun loadMedia(type: MediaType) {
        loadMedia(type.toMediaTab())
    }

    fun loadMedia(tab: MediaTab) {
        viewModelScope.launch {
            _uiState.value = MediaUiState.Loading
            when (val result = getMediaByTypeUseCase(tab)) {
                is DataResult.Success -> {
                    _uiState.value = if (result.data.isEmpty()) {
                        MediaUiState.Empty
                    } else {
                        MediaUiState.Content(result.data, tab.toMediaType())
                    }
                }
                is DataResult.Error -> _uiState.value = MediaUiState.Empty
            }
        }
    }

    fun onMediaItemClicked(item: MediaItem, isConnected: Boolean) {
        viewModelScope.launch {
            _isCasting.value = true
            try {
                val isMirroring = screenMirrorManager.isMirroring.value
                when {
                    isConnected -> {
                        if (isMirroring) screenMirrorManager.stop()
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
                            _events.emit(MediaEvent.NavigateToCast)
                        }
                    }
                    else -> _events.emit(MediaEvent.NavigateToCast)
                }
            } finally {
                _isCasting.value = false
            }
        }
    }

    private suspend fun castOrNavigate(item: MediaItem) {
        when (val result = castMediaUseCase(item)) {
            is DataResult.Success -> _castingItemId.value = item.id
            is DataResult.Error -> {
                when {
                    result.message.contains("Not connected", ignoreCase = true) ->
                        connectAndCast(item)
                    result.message.contains("Cast session error", ignoreCase = true) -> {
                        var lastError = result
                        repeat(3) {
                            delay(150)
                            when (val retry = castMediaUseCase(item)) {
                                is DataResult.Success -> {
                                    _castingItemId.value = item.id
                                    return
                                }
                                is DataResult.Error -> lastError = retry
                            }
                        }
                        _events.emit(MediaEvent.ShowError(lastError.toString()))
                    }
                    else -> _events.emit(MediaEvent.ShowError(result.message))
                }
            }
        }
    }

    private suspend fun connectAndCast(item: MediaItem) {
        val target = devices.value.firstOrNull { it.isConnected } ?: run {
            _events.emit(MediaEvent.NavigateToCast)
            return
        }
        when (connectDeviceUseCase(target.id)) {
            is DataResult.Success -> awaitConnectionAndCast(item)
            is DataResult.Error -> _events.emit(MediaEvent.NavigateToCast)
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
            _events.emit(MediaEvent.NavigateToCast)
        }
    }

    private fun MediaType.toMediaTab() = when (this) {
        MediaType.VIDEO -> MediaTab.VIDEOS
        MediaType.IMAGE -> MediaTab.PHOTOS
        MediaType.AUDIO -> MediaTab.AUDIO
    }

    private fun MediaTab.toMediaType() = when (this) {
        MediaTab.VIDEOS -> MediaType.VIDEO
        MediaTab.AUDIO -> MediaType.AUDIO
        else -> MediaType.IMAGE
    }
}

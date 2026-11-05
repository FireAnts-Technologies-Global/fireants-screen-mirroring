package com.anrstudio.template.ui.component.videocontrol

import androidx.lifecycle.viewModelScope
import com.anrstudio.template.ui.bases.BaseViewModel
import com.anrstudio.template.domain.model.ConnectionState
import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.usecase.ControlPlaybackUseCase
import com.anrstudio.template.domain.usecase.DisconnectDeviceUseCase
import com.anrstudio.template.domain.usecase.ObserveConnectionStateUseCase
import com.anrstudio.template.domain.usecase.ObservePlaybackStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoControlViewModel @Inject constructor(
    private val observePlaybackStateUseCase: ObservePlaybackStateUseCase,
    private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    private val controlPlaybackUseCase: ControlPlaybackUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(VideoControlUiState())
    val uiState: StateFlow<VideoControlUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<VideoControlEvent>()
    val events: SharedFlow<VideoControlEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                observePlaybackStateUseCase(),
                observeConnectionStateUseCase(),
            ) { playbackState, connectionState ->
                Pair(playbackState, connectionState)
            }.collect { (playbackState, connectionState) ->
                val deviceName = when (connectionState) {
                    is ConnectionState.Connected -> connectionState.device.name
                    else -> ""
                }

                _uiState.update {
                    it.copy(
                        isPlaying = playbackState.isPlaying,
                        currentPosition = playbackState.currentPosition,
                        duration = playbackState.duration,
                        title = playbackState.mediaInfo?.title ?: "",
                        thumbnailUrl = playbackState.mediaInfo?.thumbnailUrl,
                        deviceName = deviceName,
                    )
                }

                // Nếu mất kết nối, quay lại màn hình trước
                if (connectionState is ConnectionState.Disconnected || 
                    connectionState is ConnectionState.Lost) {
                    _events.emit(VideoControlEvent.NavigateBack)
                }
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (_uiState.value.isPlaying) {
                controlPlaybackUseCase.pause()
            } else {
                controlPlaybackUseCase.play()
            }
            _uiState.update { it.copy(isLoading = false) }
            
            if (result is DataResult.Error) {
                _events.emit(VideoControlEvent.ShowError(result.message))
            }
        }
    }

    fun seekTo(position: Long) {
        viewModelScope.launch {
            val result = controlPlaybackUseCase.seek(position)
            if (result is DataResult.Error) {
                _events.emit(VideoControlEvent.ShowError(result.message))
            }
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(volume = volume) }
            val result = controlPlaybackUseCase.setVolume(volume)
            if (result is DataResult.Error) {
                _events.emit(VideoControlEvent.ShowError(result.message))
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = controlPlaybackUseCase.stop()
            _uiState.update { it.copy(isLoading = false) }
            
            if (result is DataResult.Success) {
                _events.emit(VideoControlEvent.NavigateBack)
            } else if (result is DataResult.Error) {
                _events.emit(VideoControlEvent.ShowError(result.message))
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            disconnectDeviceUseCase()
            _events.emit(VideoControlEvent.NavigateBack)
        }
    }
}

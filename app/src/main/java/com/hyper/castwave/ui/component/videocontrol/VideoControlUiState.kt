package com.hyper.castwave.ui.component.videocontrol

import com.hyper.castwave.ui.bases.UiState

data class VideoControlUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val title: String = "",
    val thumbnailUrl: String? = null,
    val volume: Float = 0.5f,
    val isLoading: Boolean = false,
    val deviceName: String = "",
) : UiState

sealed interface VideoControlEvent {
    data class ShowError(val message: String) : VideoControlEvent
    data object NavigateBack : VideoControlEvent
}

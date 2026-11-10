package com.hyper.castwave.ui.component.main.home

import com.hyper.castwave.ui.bases.UiState
import com.hyper.castwave.domain.model.Channel

sealed class HomeUiState : UiState {
    data class Content(
        val isConnected: Boolean,
        val connectedDeviceName: String?,
        val isMirroring: Boolean,
        val channels: List<Channel>,
    ) : HomeUiState()
}

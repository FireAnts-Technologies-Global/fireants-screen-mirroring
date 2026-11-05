package com.anrstudio.template.ui.component.main.home

import com.anrstudio.template.ui.bases.UiState
import com.anrstudio.template.domain.model.Channel

sealed class HomeUiState : UiState {
    data class Content(
        val isConnected: Boolean,
        val connectedDeviceName: String?,
        val isMirroring: Boolean,
        val channels: List<Channel>,
    ) : HomeUiState()
}

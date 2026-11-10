package com.hyper.castwave.ui.component.cast

import com.hyper.castwave.ui.bases.UiState
import com.hyper.castwave.domain.model.CastDevice

sealed interface CastUiState : UiState {
    data object Loading : CastUiState
    data class HasDevices(
        val devices: List<CastDevice>,
        val selectedId: String?,
        val connectingId: String?,
    ) : CastUiState
    data object Empty : CastUiState
    data object LostConnection : CastUiState
    data class PinRequired(val device: CastDevice) : CastUiState
    data class Connected(
        val devices: List<CastDevice>,
        val connectedDevice: CastDevice,
        val isMirroring: Boolean,
    ) : CastUiState
}

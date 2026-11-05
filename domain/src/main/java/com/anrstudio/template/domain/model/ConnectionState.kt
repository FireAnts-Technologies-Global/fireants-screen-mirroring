package com.anrstudio.template.domain.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data class Connected(val device: CastDevice) : ConnectionState
    data object Lost : ConnectionState
    data class PinRequired(val device: CastDevice) : ConnectionState
}

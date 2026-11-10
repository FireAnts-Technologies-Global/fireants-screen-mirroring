package com.hyper.castwave.ui.component.media

sealed interface MediaEvent {
    data object NavigateToCast : MediaEvent
    data class ShowError(val message: String) : MediaEvent
}

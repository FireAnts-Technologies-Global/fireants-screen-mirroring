package com.hyper.castwave.ui.component.media

import com.hyper.castwave.ui.bases.UiState
import com.hyper.castwave.domain.model.MediaItem
import com.hyper.castwave.domain.model.MediaType

sealed interface MediaUiState : UiState {
    data object Loading : MediaUiState
    data class Content(
        val items: List<MediaItem>,
        val selectedTab: MediaType,
    ) : MediaUiState

    data object Empty : MediaUiState
    data object PermissionRequired : MediaUiState
}

sealed interface MediaEvent {
    data object NavigateToCast : MediaEvent
    data class ShowError(val message: String) : MediaEvent
}

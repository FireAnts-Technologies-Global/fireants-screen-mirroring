package com.anrstudio.template.ui.component.media

import com.anrstudio.template.ui.bases.UiState
import com.anrstudio.template.domain.model.MediaItem
import com.anrstudio.template.domain.model.MediaType

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

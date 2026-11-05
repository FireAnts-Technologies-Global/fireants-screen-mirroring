package com.anrstudio.template.ui.component.webcast

import com.anrstudio.template.ui.bases.UiState

data class WebCastUiState(
    val ipUrl: String = "",
    val isMirroring: Boolean = false,
) : UiState

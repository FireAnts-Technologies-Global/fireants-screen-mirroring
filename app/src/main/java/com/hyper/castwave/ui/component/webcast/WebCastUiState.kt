package com.hyper.castwave.ui.component.webcast

import com.hyper.castwave.ui.bases.UiState

data class WebCastUiState(
    val ipUrl: String = "",
    val isMirroring: Boolean = false,
) : UiState

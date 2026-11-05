package com.anrstudio.template.ui.component.webbrowser

import com.anrstudio.template.ui.bases.UiState
import com.anrstudio.template.domain.model.ExtractedVideo

data class WebBrowserUiState(
    val title: String = "",
    val currentUrl: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = true,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val hasVideos: Boolean = false,
    val videos: List<ExtractedVideo> = emptyList(),
) : UiState

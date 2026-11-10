package com.hyper.castwave.ui.component.webbrowser

sealed interface WebBrowserEvent {
    data object NavigateToCast : WebBrowserEvent
    data class ShowError(val message: String) : WebBrowserEvent
    data object NavigateToVideoControl : WebBrowserEvent
}

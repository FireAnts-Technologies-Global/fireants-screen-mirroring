package com.hyper.castwave.ui.component.webcast

import com.hyper.castwave.ui.bases.BaseViewModel
import com.hyper.castwave.domain.usecase.GetServerUrlUseCase
import com.hyper.castwave.domain.usecase.ObserveMirroringStateUseCase
import com.hyper.castwave.domain.usecase.StartScreenMirrorUseCase
import com.hyper.castwave.domain.usecase.StopScreenMirrorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class WebCastViewModel @Inject constructor(
    observeMirroringState: ObserveMirroringStateUseCase,
    getServerUrl: GetServerUrlUseCase,
    private val startScreenMirror: StartScreenMirrorUseCase,
    private val stopScreenMirror: StopScreenMirrorUseCase,
) : BaseViewModel() {

    val uiState: StateFlow<WebCastUiState> = combine(
        observeMirroringState(),
        getServerUrl(),
    ) { isMirroring, ipUrl ->
        WebCastUiState(ipUrl = ipUrl, isMirroring = isMirroring)
    }.stateWhileSubscribed(WebCastUiState())

    fun start(resultCode: Int, projectionData: Any) {
        startScreenMirror(resultCode, projectionData)
    }

    fun stop() {
        stopScreenMirror()
    }
}

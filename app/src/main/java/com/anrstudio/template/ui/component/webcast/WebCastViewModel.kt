package com.anrstudio.template.ui.component.webcast

import com.anrstudio.template.ui.bases.BaseViewModel
import com.anrstudio.template.domain.usecase.GetServerUrlUseCase
import com.anrstudio.template.domain.usecase.ObserveMirroringStateUseCase
import com.anrstudio.template.domain.usecase.StartScreenMirrorUseCase
import com.anrstudio.template.domain.usecase.StopScreenMirrorUseCase
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

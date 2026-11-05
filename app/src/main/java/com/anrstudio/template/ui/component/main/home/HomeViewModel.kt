package com.anrstudio.template.ui.component.main.home

import com.anrstudio.template.ui.bases.BaseViewModel
import com.anrstudio.template.domain.model.ConnectionState
import com.anrstudio.template.domain.usecase.GetChannelsUseCase
import com.anrstudio.template.domain.usecase.ObserveConnectionStateUseCase
import com.anrstudio.template.domain.usecase.ObserveDevicesUseCase
import com.anrstudio.template.domain.usecase.ObserveMirroringStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    observeDevicesUseCase: ObserveDevicesUseCase,
    observeMirroringStateUseCase: ObserveMirroringStateUseCase,
    getChannelsUseCase: GetChannelsUseCase,
) : BaseViewModel() {

    private val channels = getChannelsUseCase()

    val uiState: StateFlow<HomeUiState> = combine(
        observeConnectionStateUseCase(),
        observeDevicesUseCase(),
        observeMirroringStateUseCase(),
    ) { connectionState, devices, isMirroring ->
        val isConnected: Boolean
        val connectedDeviceName: String?
        when (connectionState) {
            is ConnectionState.Connected -> {
                isConnected = true
                connectedDeviceName = connectionState.device.name
            }
            else -> {
                val connectedDevice = devices.firstOrNull { it.isConnected }
                isConnected = connectedDevice != null
                connectedDeviceName = connectedDevice?.name
            }
        }
        HomeUiState.Content(isConnected, connectedDeviceName, isMirroring, channels)
    }.stateWhileSubscribed(HomeUiState.Content(false, null, false, channels))
}

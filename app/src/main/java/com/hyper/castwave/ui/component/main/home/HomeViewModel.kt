package com.hyper.castwave.ui.component.main.home

import com.hyper.castwave.domain.model.ConnectionState
import com.hyper.castwave.domain.usecase.DisconnectDeviceUseCase
import com.hyper.castwave.domain.usecase.GetChannelsUseCase
import com.hyper.castwave.domain.usecase.ObserveConnectionStateUseCase
import com.hyper.castwave.domain.usecase.ObserveDevicesUseCase
import com.hyper.castwave.domain.usecase.ObserveMirroringStateUseCase
import com.hyper.castwave.ui.bases.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    observeDevicesUseCase: ObserveDevicesUseCase,
    observeMirroringStateUseCase: ObserveMirroringStateUseCase,
    getChannelsUseCase: GetChannelsUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
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

    fun disconnect() {
        uiScope.launch { disconnectDeviceUseCase() }
    }
}

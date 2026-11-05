package com.anrstudio.template.ui.component.cast

import androidx.lifecycle.viewModelScope
import com.anrstudio.template.ui.bases.BaseViewModel
import com.anrstudio.template.domain.model.ConnectionState
import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.usecase.ConnectDeviceUseCase
import com.anrstudio.template.domain.usecase.ConnectDeviceWithPinUseCase
import com.anrstudio.template.domain.usecase.DisconnectDeviceUseCase
import com.anrstudio.template.domain.usecase.ObserveConnectionStateUseCase
import com.anrstudio.template.domain.usecase.ObserveDevicesUseCase
import com.anrstudio.template.domain.usecase.RequestPinUseCase
import com.anrstudio.template.domain.usecase.StartDiscoveryUseCase
import com.anrstudio.template.domain.usecase.StopDiscoveryUseCase
import com.anrstudio.template.casting.mirroring.ScreenMirrorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CastViewModel @Inject constructor(
    private val startDiscoveryUseCase: StartDiscoveryUseCase,
    private val stopDiscoveryUseCase: StopDiscoveryUseCase,
    observeDevicesUseCase: ObserveDevicesUseCase,
    observeConnectionStateUseCase: ObserveConnectionStateUseCase,
    private val connectDeviceUseCase: ConnectDeviceUseCase,
    private val connectDeviceWithPinUseCase: ConnectDeviceWithPinUseCase,
    private val requestPinUseCase: RequestPinUseCase,
    private val disconnectDeviceUseCase: DisconnectDeviceUseCase,
    private val screenMirrorManager: ScreenMirrorManager,
) : BaseViewModel() {

    private val _selectedDeviceId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CastUiState> = combine(
        observeDevicesUseCase(),
        observeConnectionStateUseCase(),
        _selectedDeviceId,
        screenMirrorManager.isMirroring,
    ) { devices, connectionState, selectedId, isMirroring ->
        when {
            connectionState is ConnectionState.Connected -> {
                val updatedDevices = devices.map { device ->
                    if (device.id == connectionState.device.id) {
                        device.copy(isConnected = true)
                    } else {
                        device.copy(isConnected = false)
                    }
                }
                CastUiState.Connected(updatedDevices, connectionState.device, isMirroring)
            }

            connectionState is ConnectionState.PinRequired ->
                CastUiState.PinRequired(connectionState.device)

            connectionState is ConnectionState.Lost ->
                CastUiState.LostConnection

            connectionState is ConnectionState.Connecting ->
                CastUiState.HasDevices(devices, selectedId, connectingId = selectedId)

            devices.isEmpty() ->
                CastUiState.Empty

            else ->
                CastUiState.HasDevices(devices, selectedId, connectingId = null)
        }
    }.stateWhileSubscribed(CastUiState.Loading)

    init {
        startDiscovery()
    }

    fun refresh() {
        startDiscovery()
    }

    fun connectDevice(deviceId: String) {
        viewModelScope.launch {
            _selectedDeviceId.value = deviceId
            val result = connectDeviceUseCase(deviceId)
            if (result is DataResult.Error) {
                _selectedDeviceId.value = null
            }
        }
    }

    fun requestPinForDevice(deviceId: String) {
        _selectedDeviceId.value = deviceId
        viewModelScope.launch { requestPinUseCase(deviceId) }
    }

    fun submitPin(pin: String) {
        val deviceId = _selectedDeviceId.value ?: return
        viewModelScope.launch { connectDeviceWithPinUseCase(deviceId, pin) }
    }

    fun cancelPin() {
        _selectedDeviceId.value = null
        viewModelScope.launch { disconnectDeviceUseCase() }
    }

    fun stopMirroring() {
        screenMirrorManager.stop()
    }

    fun disconnect() {
        viewModelScope.launch { disconnectDeviceUseCase() }
    }

    private fun startDiscovery() {
        viewModelScope.launch { startDiscoveryUseCase() }
    }

    override fun onCleared() {
        super.onCleared()
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            stopDiscoveryUseCase()
        }
    }
}

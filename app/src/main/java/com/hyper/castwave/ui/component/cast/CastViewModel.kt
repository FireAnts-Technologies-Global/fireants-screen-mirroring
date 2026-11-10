package com.hyper.castwave.ui.component.cast

import androidx.lifecycle.viewModelScope
import com.hyper.castwave.ui.bases.BaseViewModel
import com.hyper.castwave.domain.model.ConnectionState
import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.usecase.ConnectDeviceUseCase
import com.hyper.castwave.domain.usecase.ConnectDeviceWithPinUseCase
import com.hyper.castwave.domain.usecase.DisconnectDeviceUseCase
import com.hyper.castwave.domain.usecase.ObserveConnectionStateUseCase
import com.hyper.castwave.domain.usecase.ObserveDevicesUseCase
import com.hyper.castwave.domain.usecase.RequestPinUseCase
import com.hyper.castwave.domain.usecase.StartDiscoveryUseCase
import com.hyper.castwave.domain.usecase.StopDiscoveryUseCase
import com.hyper.castwave.casting.mirroring.ScreenMirrorManager
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

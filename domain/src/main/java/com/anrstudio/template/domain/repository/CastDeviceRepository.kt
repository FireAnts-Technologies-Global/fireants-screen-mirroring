package com.anrstudio.template.domain.repository

import com.anrstudio.template.domain.model.CastDevice
import com.anrstudio.template.domain.model.ConnectionState
import com.anrstudio.template.domain.model.DataResult
import kotlinx.coroutines.flow.Flow

interface CastDeviceRepository {
    fun observeDevices(): Flow<List<CastDevice>>
    fun observeConnectionState(): Flow<ConnectionState>
    suspend fun startDiscovery()
    suspend fun stopDiscovery()
    suspend fun connect(deviceId: String): DataResult<Unit>
    suspend fun connectWithPin(deviceId: String, pin: String): DataResult<Unit>
    suspend fun requestPin(deviceId: String): DataResult<Unit>
    suspend fun disconnect()
}

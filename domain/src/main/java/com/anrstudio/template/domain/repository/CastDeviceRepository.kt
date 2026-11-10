package com.hyper.castwave.domain.repository

import com.hyper.castwave.domain.model.CastDevice
import com.hyper.castwave.domain.model.ConnectionState
import com.hyper.castwave.domain.model.DataResult
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

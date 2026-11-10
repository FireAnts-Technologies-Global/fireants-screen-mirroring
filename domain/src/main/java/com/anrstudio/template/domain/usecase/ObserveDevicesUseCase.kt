package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.CastDevice
import com.hyper.castwave.domain.repository.CastDeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDevicesUseCase @Inject constructor(
    private val repository: CastDeviceRepository,
) {
    operator fun invoke(): Flow<List<CastDevice>> = repository.observeDevices()
}

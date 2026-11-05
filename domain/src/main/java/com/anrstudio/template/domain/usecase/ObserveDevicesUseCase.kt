package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.CastDevice
import com.anrstudio.template.domain.repository.CastDeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDevicesUseCase @Inject constructor(
    private val repository: CastDeviceRepository,
) {
    operator fun invoke(): Flow<List<CastDevice>> = repository.observeDevices()
}

package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.DataResult
import com.anrstudio.template.domain.repository.CastDeviceRepository
import javax.inject.Inject

class ConnectDeviceWithPinUseCase @Inject constructor(
    private val repository: CastDeviceRepository,
) {
    suspend operator fun invoke(deviceId: String, pin: String): DataResult<Unit> =
        repository.connectWithPin(deviceId, pin)
}

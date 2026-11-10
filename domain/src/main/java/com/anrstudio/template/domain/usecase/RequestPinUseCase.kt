package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.DataResult
import com.hyper.castwave.domain.repository.CastDeviceRepository
import javax.inject.Inject

class RequestPinUseCase @Inject constructor(
    private val repository: CastDeviceRepository,
) {
    suspend operator fun invoke(deviceId: String): DataResult<Unit> =
        repository.requestPin(deviceId)
}

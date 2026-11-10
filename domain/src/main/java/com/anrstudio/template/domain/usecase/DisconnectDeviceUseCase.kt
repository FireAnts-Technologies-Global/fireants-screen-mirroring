package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.repository.CastDeviceRepository
import javax.inject.Inject

class DisconnectDeviceUseCase @Inject constructor(
    private val repository: CastDeviceRepository,
) {
    suspend operator fun invoke() = repository.disconnect()
}

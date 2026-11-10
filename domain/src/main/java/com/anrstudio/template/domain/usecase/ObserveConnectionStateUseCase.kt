package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.ConnectionState
import com.hyper.castwave.domain.repository.CastDeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionStateUseCase @Inject constructor(
    private val repository: CastDeviceRepository,
) {
    operator fun invoke(): Flow<ConnectionState> = repository.observeConnectionState()
}

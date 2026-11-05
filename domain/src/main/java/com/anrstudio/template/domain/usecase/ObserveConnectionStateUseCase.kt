package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.ConnectionState
import com.anrstudio.template.domain.repository.CastDeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveConnectionStateUseCase @Inject constructor(
    private val repository: CastDeviceRepository,
) {
    operator fun invoke(): Flow<ConnectionState> = repository.observeConnectionState()
}

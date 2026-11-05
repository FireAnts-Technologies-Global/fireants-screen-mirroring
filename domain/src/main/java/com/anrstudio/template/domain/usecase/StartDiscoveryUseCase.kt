package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.repository.CastDeviceRepository
import javax.inject.Inject

class StartDiscoveryUseCase @Inject constructor(
    private val repository: CastDeviceRepository,
) {
    suspend operator fun invoke() = repository.startDiscovery()
}

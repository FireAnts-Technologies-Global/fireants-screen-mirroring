package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.repository.ScreenMirrorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetServerUrlUseCase @Inject constructor(
    private val repository: ScreenMirrorRepository,
) {
    operator fun invoke(): Flow<String> = repository.serverUrl
}

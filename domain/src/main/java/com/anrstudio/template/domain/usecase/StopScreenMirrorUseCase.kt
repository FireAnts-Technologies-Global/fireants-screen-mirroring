package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.repository.ScreenMirrorRepository
import javax.inject.Inject

class StopScreenMirrorUseCase @Inject constructor(
    private val repository: ScreenMirrorRepository,
) {
    operator fun invoke() {
        repository.stop()
    }
}

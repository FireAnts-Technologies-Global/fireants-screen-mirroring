package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.repository.ScreenMirrorRepository
import javax.inject.Inject

class StopScreenMirrorUseCase @Inject constructor(
    private val repository: ScreenMirrorRepository,
) {
    operator fun invoke() {
        repository.stop()
    }
}

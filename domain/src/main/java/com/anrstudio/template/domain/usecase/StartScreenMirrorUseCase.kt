package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.repository.ScreenMirrorRepository
import javax.inject.Inject

class StartScreenMirrorUseCase @Inject constructor(
    private val repository: ScreenMirrorRepository,
) {
    operator fun invoke(resultCode: Int, projectionData: Any) {
        repository.start(resultCode, projectionData)
    }
}

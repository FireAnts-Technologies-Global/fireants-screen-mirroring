package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.repository.ScreenMirrorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMirroringStateUseCase @Inject constructor(
    private val repository: ScreenMirrorRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.isMirroring
}

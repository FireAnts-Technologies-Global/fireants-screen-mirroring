package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.repository.MediaRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    suspend operator fun invoke(id: String) = repository.toggleFavorite(id)
}

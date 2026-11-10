package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.repository.MediaRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: MediaRepository,
) {
    suspend operator fun invoke(id: String) = repository.toggleFavorite(id)
}

package com.anrstudio.template.domain.usecase

import com.anrstudio.template.domain.model.Channel
import com.anrstudio.template.domain.repository.ChannelRepository
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val repository: ChannelRepository,
) {
    operator fun invoke(): List<Channel> = repository.getChannels()
}

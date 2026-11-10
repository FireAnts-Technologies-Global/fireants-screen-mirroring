package com.hyper.castwave.domain.usecase

import com.hyper.castwave.domain.model.Channel
import com.hyper.castwave.domain.repository.ChannelRepository
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val repository: ChannelRepository,
) {
    operator fun invoke(): List<Channel> = repository.getChannels()
}

package com.hyper.castwave.domain.repository

import com.hyper.castwave.domain.model.Channel

interface ChannelRepository {
    fun getChannels(): List<Channel>
}

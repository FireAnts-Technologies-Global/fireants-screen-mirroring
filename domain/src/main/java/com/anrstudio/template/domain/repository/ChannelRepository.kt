package com.anrstudio.template.domain.repository

import com.anrstudio.template.domain.model.Channel

interface ChannelRepository {
    fun getChannels(): List<Channel>
}

package com.anrstudio.template.data.repository

import com.anrstudio.template.domain.model.Channel
import com.anrstudio.template.domain.model.ChannelType
import com.anrstudio.template.domain.repository.ChannelRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor() : ChannelRepository {

    override fun getChannels(): List<Channel> = listOf(
        Channel("youtube", "YouTube", 0, "https://m.youtube.com", ChannelType.YOUTUBE),
        Channel("vimeo", "Vimeo", 0, "https://vimeo.com", ChannelType.VIMEO),
        Channel("twitch", "Twitch", 0, "https://m.twitch.tv", ChannelType.TWITCH),
        Channel("google", "Google", 0, "https://www.google.com", ChannelType.GOOGLE),
        Channel("internet", "Internet", 0, "https://www.google.com", ChannelType.INTERNET),
        Channel("iptv", "IPTV", 0, "https://www.google.com/search?q=iptv+stream", ChannelType.IPTV),
        Channel("tiktok", "TikTok", 0, "https://www.tiktok.com", ChannelType.TIKTOK),
    )
}

package com.hyper.castwave.ui.component.iptv.player.viewmodel

import android.content.Context
import com.hyper.castwave.data.iptv.model.TVChannel

interface ChannelEvent {
    class GetAllChanelForGroup(
        val context: Context,
        val tvChannel: TVChannel,
        val groupType: String = "category",
        val filterType: String = "",
    ) : ChannelEvent
    class SearchChanelByKeyword(
        val context: Context,
        val tvChannel: TVChannel,
        val strSearch: String,
        val groupType: String = "category",
        val filterType: String = "",
    ) : ChannelEvent
}

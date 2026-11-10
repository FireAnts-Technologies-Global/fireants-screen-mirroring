package com.hyper.castwave.ui.component.iptv.player.viewmodel

import android.content.Context
import com.hyper.castwave.ui.component.iptv.IptvAppConstants
import com.hyper.castwave.data.iptv.database.manager.ChannelManager
import com.hyper.castwave.data.iptv.model.DataState
import com.hyper.castwave.data.iptv.model.TVChannel
import com.hyper.castwave.ui.bases.BaseViewModel
import com.hyper.castwave.ui.bases.ext.normalText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channelManager: ChannelManager,
) : BaseViewModel() {
    private val _dataTVChannelState = MutableStateFlow<DataState<TVChannel>>(DataState.None)
    val dataTVChannelState: StateFlow<DataState<TVChannel>> = _dataTVChannelState
    fun onEvent(event: ChannelEvent) {
        when (event) {
            is ChannelEvent.GetAllChanelForGroup -> searchChanelByKeyword(
                event.context,
                event.tvChannel,
                "",
                event.groupType,
                event.filterType
            )

            is ChannelEvent.SearchChanelByKeyword -> searchChanelByKeyword(
                event.context,
                event.tvChannel,
                event.strSearch,
                event.groupType,
                event.filterType
            )
        }
    }

    private var searchJob: Job? = null

    private fun searchChanelByKeyword(
        context: Context,
        tvChannel: TVChannel,
        keyword: String,
        groupType: String = IptvAppConstants.GROUP_TYPE_CATEGORY,
        filterType: String = "",
    ) {
        searchJob?.cancel()
        searchJob = bgScope.launch {
            _dataTVChannelState.update { DataState.Loading }
            try {
                val channelsFlow = when {
                    filterType == IptvAppConstants.FILTER_TYPE_FAVORITE -> {
                        channelManager.getFavoriteChannels()
                    }

                    filterType == IptvAppConstants.FILTER_TYPE_RECENT -> {
                        channelManager.getRecentChannels()
                    }

                    filterType == IptvAppConstants.FILTER_ALL -> {
                        channelManager.getAllChannels()
                    }

                    groupType == IptvAppConstants.GROUP_TYPE_SOURCE -> {
                        channelManager.getPlayListForSourceById(tvChannel.sourceId)
                    }

                    else -> {
                        channelManager.getPlayListForGroupById(tvChannel.groupId)
                    }
                }

                channelsFlow
                    .take(1)
                    .catch { ex ->
                        _dataTVChannelState.update { DataState.Error(ex.message ?: "") }
                    }
                    .collect { list ->
                        if (keyword.isBlank()) _dataTVChannelState.update { DataState.Success(list) }
                        else {
                            val keySearch = keyword.normalText()
                            val result = list.filter { it.name.normalText().contains(keySearch) }
                            _dataTVChannelState.update { DataState.Success(result) }
                        }
                    }
            } catch (ex: Exception) {
                _dataTVChannelState.update { DataState.Error(ex.message ?: "") }
            }

        }
    }

}

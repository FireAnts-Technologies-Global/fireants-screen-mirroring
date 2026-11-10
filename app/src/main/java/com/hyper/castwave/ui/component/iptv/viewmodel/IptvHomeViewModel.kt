package com.hyper.castwave.ui.component.iptv.viewmodel

import com.hyper.castwave.data.iptv.database.manager.ChannelDataManager
import com.hyper.castwave.data.iptv.model.TVChannel
import com.hyper.castwave.data.iptv.model.TVChannelGroup
import com.hyper.castwave.data.iptv.model.TVChannelSource
import com.hyper.castwave.data.iptv.model.TVChannelSourceType
import com.hyper.castwave.ui.bases.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupWithCount(
    val group: TVChannelGroup,
    val channelCount: Int,
    val sourceType: String,
    val importDate: Long = 0L, // For sorting by time
)

@HiltViewModel
class IptvHomeViewModel @Inject constructor(
    private val channelDataManager: ChannelDataManager ,
) : BaseViewModel() {

    val allChannels: Flow<List<TVChannel>> = channelDataManager.getAllTVChannel()
    val favoriteChannels: Flow<List<TVChannel>> = channelDataManager.getFavoriteTVChannels()
    val recentChannels: Flow<List<TVChannel>> = channelDataManager.getRecentTVChannels()

    val allGroups: Flow<List<GroupWithCount>> = combine(
        getGroupsByType(TVChannelSourceType.URL.value),
        getGroupsByType(TVChannelSourceType.STREAM.value),
        getGroupsByType(TVChannelSourceType.FILE.value),
        getGroupsByType(TVChannelSourceType.DEVICE.value)
    ) { urlGroups, streamGroups, fileGroups, deviceGroups ->
        (urlGroups + streamGroups + fileGroups + deviceGroups)
            .sortedByDescending { it.importDate }
    }

    val urlGroups: Flow<List<GroupWithCount>> = getUrlGroupsBySource()
    val streamGroups: Flow<List<GroupWithCount>> = getGroupsBySource(TVChannelSourceType.STREAM.value)
    val fileGroups: Flow<List<GroupWithCount>> = getGroupsBySource(TVChannelSourceType.FILE.value)
    val deviceGroups: Flow<List<GroupWithCount>> = getGroupsBySource(TVChannelSourceType.DEVICE.value)

    val allGroupsBySource: Flow<List<GroupWithCount>> = combine(
        getUrlGroupsBySource(),
        getGroupsBySource(TVChannelSourceType.STREAM.value),
        getGroupsBySource(TVChannelSourceType.FILE.value),
        getGroupsBySource(TVChannelSourceType.DEVICE.value)
    ) { urlGroups, streamGroups, fileGroups, deviceGroups ->
        (urlGroups + streamGroups + fileGroups + deviceGroups)
            .sortedByDescending { it.importDate }
    }

    // Get all sources grouped by import (not by type)
    val allSourcesByImport: Flow<List<GroupWithCount>> = channelDataManager.getAllTVChannelSource()
        .flatMapLatest { sources ->
            if (sources.isEmpty()) {
                flowOf(emptyList())
            } else {
                val groupFlows = sources.map { source ->
                    channelDataManager.getTVChannelsBySource(source.id)
                        .map { channels ->
                            val virtualGroup = TVChannelGroup(
                                name = "source_${source.id}",
                                displayName = source.name,
                                channelNumber = 0,
                                isFavorite = false,
                                sourceId = source.id
                            )
                            GroupWithCount(
                                group = virtualGroup,
                                channelCount = channels.size,
                                sourceType = source.sourceType,
                                importDate = source.importDate
                            )
                        }
                }
                combine(groupFlows) { it.toList() }
                    .map { groups -> groups.sortedByDescending { it.importDate } }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getUrlGroupsBySource(): Flow<List<GroupWithCount>> {
        return getGroupsBySource(TVChannelSourceType.URL.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getGroupsBySource(sourceType: String): Flow<List<GroupWithCount>> {
        return channelDataManager.getTVChannelSourcesByType(sourceType)
            .flatMapLatest { sources ->
                if (sources.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val groupFlows = sources.map { source ->
                        channelDataManager.getTVChannelsBySource(source.id)
                            .map { channels ->
                                val virtualGroup = TVChannelGroup(
                                    name = "source_${source.id}",
                                    displayName = source.name,
                                    channelNumber = 0,
                                    isFavorite = false,
                                    sourceId = source.id
                                )
                                GroupWithCount(
                                    group = virtualGroup,
                                    channelCount = channels.size,
                                    sourceType = sourceType,
                                    importDate = source.importDate
                                )
                            }
                    }
                    combine(groupFlows) { it.toList() }
                        .map { groups -> groups.sortedByDescending { it.importDate } }
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getGroupsByType(sourceType: String): Flow<List<GroupWithCount>> {
        return channelDataManager.getTVChannelSourcesByType(sourceType)
            .flatMapLatest { sources ->
                if (sources.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    // Create a map of sourceId to importDate for quick lookup
                    val sourceImportDateMap = sources.associate { it.id to it.importDate }
                    
                    val groupFlows = sources.map { source ->
                        channelDataManager.getTVChannelGroupsBySource(source.id)
                            .flatMapLatest { groups ->
                                if (groups.isEmpty()) {
                                    flowOf(emptyList<GroupWithCount>())
                                } else {
                                    val groupWithCountFlows = groups.map { group ->
                                        channelDataManager.getTVChannelsByGroup(group.name)
                                            .map { channels ->
                                                GroupWithCount(
                                                    group = group,
                                                    channelCount = channels.size,
                                                    sourceType = sourceType,
                                                    importDate = sourceImportDateMap[group.sourceId] ?: 0L
                                                )
                                            }
                                    }
                                    combine(groupWithCountFlows) { it.toList() }
                                }
                            }
                    }
                    combine(groupFlows) { arrays ->
                        arrays.flatMap { it }
                            .sortedByDescending { it.importDate }
                    }
                }
            }
    }
    suspend fun updateChannel(channel: TVChannel) {
        uiScope.launch {
            channelDataManager.updateTVChannel(channel)
        }
    }
    
    suspend fun deleteChannel(channel: TVChannel) {
        uiScope.launch {
            channelDataManager.deleteTVChannel(channel)
        }
    }
    
    suspend fun updateChannelFavorite(channel: TVChannel, isFavorite: Boolean) {
        uiScope.launch {
            channelDataManager.updateTVChannel(channel.copy(
                isFavorite = isFavorite
            ))
        }
    }

    suspend fun getChannelSourceById(id: Long): TVChannelSource? {
        return channelDataManager.getTVChannelSourceById(id).first()
    }
    
    suspend fun updatePlaylist(source: TVChannelSource) {
        uiScope.launch {
            channelDataManager.updateTVChannelSource(source)
        }
    }
    suspend fun deletePlaylist(source: TVChannelSource) {
        uiScope.launch {
            channelDataManager.deleteTVChannelSource(source)
        }
    }
    suspend fun getAllChannelGroups(): List<TVChannelGroup> {
        return channelDataManager.getAllTVChannelGroup().first()
    }
    fun searchTVChannels(query: String): Flow<List<TVChannel>> {
        return channelDataManager.searchTVChannels(query)
    }
    
    fun getTVChannelsBySource(sourceId: Long): Flow<List<TVChannel>> {
        return channelDataManager.getTVChannelsBySource(sourceId)
    }
}


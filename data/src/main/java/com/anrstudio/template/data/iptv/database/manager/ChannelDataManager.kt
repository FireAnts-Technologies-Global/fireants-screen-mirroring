package com.hyper.castwave.data.iptv.database.manager

import com.hyper.castwave.data.iptv.database.repository.ChannelDaoRepository
import com.hyper.castwave.data.iptv.model.TVChannel
import com.hyper.castwave.data.iptv.model.TVChannelGroup
import com.hyper.castwave.data.iptv.model.TVChannelSource
import com.hyper.castwave.data.iptv.pref.AppSharedPref
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelDataManager @Inject constructor(
    private val appSharedPref: AppSharedPref,
    private val channelDaoRepository: ChannelDaoRepository
) {

    fun getAllTVChannel(): Flow<List<TVChannel>> = channelDaoRepository.getAllTVChannel()

    fun getAllTVChannelSourceNames(): Flow<List<String>> =
        channelDaoRepository.getAllTVChannelSourceNames()

    fun getTVChannelsBySource(sourceId: Long): Flow<List<TVChannel>> =
        channelDaoRepository.getTVChannelsBySource(sourceId)

    fun getTVChannelsByGroup(groupId: String): Flow<List<TVChannel>> =
        channelDaoRepository.getTVChannelsByGroup(groupId)

    fun getFavoriteTVChannels(): Flow<List<TVChannel>> =
        channelDaoRepository.getFavoriteTVChannels()

    fun getRecentTVChannels(): Flow<List<TVChannel>> =
        channelDaoRepository.getRecentTVChannels()

    fun searchTVChannels(query: String): Flow<List<TVChannel>> =
        channelDaoRepository.searchTVChannels(query)

    suspend fun insertTVChannel(tvChannel: TVChannel) =
        channelDaoRepository.insertTVChannel(tvChannel)

    suspend fun insertTVChannels(tvChannels: List<TVChannel>) =
        channelDaoRepository.insertTVChannels(tvChannels)

    suspend fun updateTVChannel(tvChannel: TVChannel) =
        channelDaoRepository.updateTVChannel(tvChannel)

    suspend fun deleteTVChannel(tvChannel: TVChannel) =
        channelDaoRepository.deleteTVChannel(tvChannel)

    suspend fun deleteTVChannelsBySource(sourceId: Long) =
        channelDaoRepository.deleteTVChannelsBySource(sourceId)

    suspend fun deleteTVChannelsByGroup(groupId: String) =
        channelDaoRepository.deleteTVChannelsByGroup(groupId)

    fun getAllTVChannelGroup(): Flow<List<TVChannelGroup>> =
        channelDaoRepository.getAllTVChannelGroup()

    fun getTVChannelGroupsBySource(sourceId: Long): Flow<List<TVChannelGroup>> =
        channelDaoRepository.getTVChannelGroupsBySource(sourceId)

    suspend fun insertTVChannelGroup(tvChannelGroup: TVChannelGroup) =
        channelDaoRepository.insertTVChannelGroup(tvChannelGroup)

    suspend fun insertTVChannelGroups(tvChannelGroups: List<TVChannelGroup>) =
        channelDaoRepository.insertTVChannelGroups(tvChannelGroups)

    suspend fun updateTVChannelGroup(tvChannelGroup: TVChannelGroup) =
        channelDaoRepository.updateTVChannelGroup(tvChannelGroup)

    suspend fun deleteTVChannelGroup(tvChannelGroup: TVChannelGroup) =
        channelDaoRepository.deleteTVChannelGroup(tvChannelGroup)

    suspend fun deleteTVChannelGroupsBySource(sourceId: Long) =
        channelDaoRepository.deleteTVChannelGroupsBySource(sourceId)

    fun getAllTVChannelSource(): Flow<List<TVChannelSource>> =
        channelDaoRepository.getAllTVChannelSource()

    fun getTVChannelSourceById(id: Long): Flow<TVChannelSource?> =
        channelDaoRepository.getTVChannelSourceById(id)

    fun getTVChannelSourcesByType(sourceType: String): Flow<List<TVChannelSource>> =
        channelDaoRepository.getTVChannelSourcesByType(sourceType)

    fun getActiveTVChannelSources(): Flow<List<TVChannelSource>> =
        channelDaoRepository.getActiveTVChannelSources()

    suspend fun insertTVChannelSource(tvChannelSource: TVChannelSource): Long =
        channelDaoRepository.insertTVChannelSource(tvChannelSource)

    suspend fun getTVChannelSourceByName(name: String): TVChannelSource? =
        channelDaoRepository.getTvChanelByName(name)

    suspend fun updateTVChannelSource(tvChannelSource: TVChannelSource) =
        channelDaoRepository.updateTVChannelSource(tvChannelSource)

    suspend fun deleteTVChannelSource(tvChannelSource: TVChannelSource) =
        channelDaoRepository.deleteTVChannelSource(tvChannelSource)

    suspend fun deleteTVChannelSourceById(id: Long) =
        channelDaoRepository.deleteTVChannelSourceById(id)

    suspend fun getChannelCountBySource(sourceId: Long): Int =
        channelDaoRepository.getChannelCountBySource(sourceId)

    suspend fun getGroupCountBySource(sourceId: Long): Int =
        channelDaoRepository.getGroupCountBySource(sourceId)
}


package com.anrstudio.template.data.iptv.database.repository

import com.anrstudio.template.data.iptv.model.TVChannel
import com.anrstudio.template.data.iptv.model.TVChannelGroup
import com.anrstudio.template.data.iptv.model.TVChannelSource
import kotlinx.coroutines.flow.Flow

interface ChannelDaoRepositoryInterface {

    fun getAllTVChannel(): Flow<List<TVChannel>>
    fun getTVChannelsBySource(sourceId: Long): Flow<List<TVChannel>>
    fun getTVChannelsByGroup(groupId: String): Flow<List<TVChannel>>
    fun getFavoriteTVChannels(): Flow<List<TVChannel>>
    fun getRecentTVChannels(): Flow<List<TVChannel>>
    fun searchTVChannels(query: String): Flow<List<TVChannel>>

    suspend fun insertTVChannel(tvChannel: TVChannel)
    suspend fun insertTVChannels(tvChannels: List<TVChannel>)
    suspend fun updateTVChannel(tvChannel: TVChannel)
    suspend fun deleteTVChannel(tvChannel: TVChannel)
    suspend fun deleteTVChannelsBySource(sourceId: Long)
    suspend fun deleteTVChannelsByGroup(groupId: String)
    fun getAllTVChannelGroup(): Flow<List<TVChannelGroup>>
    fun getTVChannelGroupsBySource(sourceId: Long): Flow<List<TVChannelGroup>>
    suspend fun insertTVChannelGroup(tvChannelGroup: TVChannelGroup)
    suspend fun insertTVChannelGroups(tvChannelGroups: List<TVChannelGroup>)
    suspend fun updateTVChannelGroup(tvChannelGroup: TVChannelGroup)
    suspend fun deleteTVChannelGroup(tvChannelGroup: TVChannelGroup)
    suspend fun deleteTVChannelGroupsBySource(sourceId: Long)
    fun getAllTVChannelSource(): Flow<List<TVChannelSource>>
    fun getAllTVChannelSourceNames(): Flow<List<String>>
    fun getTVChannelSourceById(id: Long): Flow<TVChannelSource?>
    fun getTVChannelSourcesByType(sourceType: String): Flow<List<TVChannelSource>>
    fun getActiveTVChannelSources(): Flow<List<TVChannelSource>>
    suspend fun insertTVChannelSource(tvChannelSource: TVChannelSource): Long
    suspend fun updateTVChannelSource(tvChannelSource: TVChannelSource)
    suspend fun deleteTVChannelSource(tvChannelSource: TVChannelSource)
    suspend fun deleteTVChannelSourceById(id: Long)
    suspend fun getChannelCountBySource(sourceId: Long): Int
    suspend fun getGroupCountBySource(sourceId: Long): Int

    suspend fun getTvChanelByName(name: String): TVChannelSource?
}

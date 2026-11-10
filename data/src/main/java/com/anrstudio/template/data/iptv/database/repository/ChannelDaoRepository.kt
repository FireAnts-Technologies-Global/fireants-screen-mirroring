package com.hyper.castwave.data.iptv.database.repository

import com.hyper.castwave.data.iptv.database.dao.ChannelDao
import com.hyper.castwave.data.iptv.model.TVChannel
import com.hyper.castwave.data.iptv.model.TVChannelGroup
import com.hyper.castwave.data.iptv.model.TVChannelSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class ChannelDaoRepository @Inject constructor(
    private val dao: ChannelDao
) : ChannelDaoRepositoryInterface {

    override fun getAllTVChannel(): Flow<List<TVChannel>> {
        return dao.getAllTVChannel()
    }

    override fun getTVChannelsBySource(sourceId: Long): Flow<List<TVChannel>> {
        return dao.getTVChannelsBySource(sourceId)
    }

    override fun getTVChannelsByGroup(groupId: String): Flow<List<TVChannel>> {
        return dao.getTVChannelsByGroup(groupId)
    }

    override fun getFavoriteTVChannels(): Flow<List<TVChannel>> {
        return dao.getFavoriteTVChannels()
    }

    override fun getRecentTVChannels(): Flow<List<TVChannel>> {
        return dao.getRecentTVChannels()
    }

    override fun searchTVChannels(query: String): Flow<List<TVChannel>> {
        val searchQuery = "%$query%"
        return dao.searchTVChannels(searchQuery)
    }

    override suspend fun insertTVChannel(tvChannel: TVChannel) {
        try {
            withContext(Dispatchers.IO) {
                dao.insertTVChannel(tvChannel)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("insertTVChannel failed by ${ex.message}")
        }
    }

    override suspend fun insertTVChannels(tvChannels: List<TVChannel>) {
        try {
            withContext(Dispatchers.IO) {
                dao.insertTVChannels(tvChannels)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("insertTVChannels failed by ${ex.message}")
        }
    }

    override suspend fun updateTVChannel(tvChannel: TVChannel) {
        try {
            withContext(Dispatchers.IO) {
                dao.updateTVChannel(tvChannel)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("updateTVChannel failed by ${ex.message}")
        }
    }

    override suspend fun deleteTVChannel(tvChannel: TVChannel) {
        try {
            withContext(Dispatchers.IO) {
                dao.deleteTVChannel(tvChannel)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("deleteTVChannel failed by ${ex.message}")
        }
    }

    override suspend fun deleteTVChannelsBySource(sourceId: Long) {
        try {
            withContext(Dispatchers.IO) {
                dao.deleteTVChannelsBySource(sourceId)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("deleteTVChannelsBySource failed by ${ex.message}")
        }
    }

    override suspend fun deleteTVChannelsByGroup(groupId: String) {
        try {
            withContext(Dispatchers.IO) {
                dao.deleteTVChannelsByGroup(groupId)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("deleteTVChannelsByGroup failed by ${ex.message}")
        }
    }

    override fun getAllTVChannelGroup(): Flow<List<TVChannelGroup>> {
        return dao.getAllTVChannelGroup()
    }

    override fun getTVChannelGroupsBySource(sourceId: Long): Flow<List<TVChannelGroup>> {
        return dao.getTVChannelGroupsBySource(sourceId)
    }

    override suspend fun insertTVChannelGroup(tvChannelGroup: TVChannelGroup) {
        try {
            withContext(Dispatchers.IO) {
                dao.insertTVChannelGroup(tvChannelGroup)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("insertTVChannelGroup failed by ${ex.message}")
        }
    }

    override suspend fun insertTVChannelGroups(tvChannelGroups: List<TVChannelGroup>) {
        try {
            withContext(Dispatchers.IO) {
                dao.insertTVChannelGroups(tvChannelGroups)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("insertTVChannelGroups failed by ${ex.message}")
        }
    }

    override suspend fun updateTVChannelGroup(tvChannelGroup: TVChannelGroup) {
        try {
            withContext(Dispatchers.IO) {
                dao.updateTVChannelGroup(tvChannelGroup)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("updateTVChannelGroup failed by ${ex.message}")
        }
    }

    override suspend fun deleteTVChannelGroup(tvChannelGroup: TVChannelGroup) {
        try {
            withContext(Dispatchers.IO) {
                dao.deleteTVChannelGroup(tvChannelGroup)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("deleteTVChannelGroup failed by ${ex.message}")
        }
    }

    override suspend fun deleteTVChannelGroupsBySource(sourceId: Long) {
        try {
            withContext(Dispatchers.IO) {
                dao.deleteTVChannelGroupsBySource(sourceId)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("deleteTVChannelGroupsBySource failed by ${ex.message}")
        }
    }

    override fun getAllTVChannelSource(): Flow<List<TVChannelSource>> {
        return dao.getAllTVChannelSource()
    }

    override fun getAllTVChannelSourceNames(): Flow<List<String>> {
        return dao.getAllTVChannelSourceNames()
    }

    override fun getTVChannelSourceById(id: Long): Flow<TVChannelSource?> {
        return dao.getTVChannelSourceById(id)
    }

    override fun getTVChannelSourcesByType(sourceType: String): Flow<List<TVChannelSource>> {
        return dao.getTVChannelSourcesByType(sourceType)
    }

    override fun getActiveTVChannelSources(): Flow<List<TVChannelSource>> {
        return dao.getActiveTVChannelSources()
    }

    override suspend fun insertTVChannelSource(tvChannelSource: TVChannelSource): Long {
        return try {
            withContext(Dispatchers.IO) {
                dao.insertTVChannelSource(tvChannelSource)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("insertTVChannelSource failed by ${ex.message}")
            0L
        }
    }

    override suspend fun updateTVChannelSource(tvChannelSource: TVChannelSource) {
        try {
            withContext(Dispatchers.IO) {
                dao.updateTVChannelSource(tvChannelSource)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("updateTVChannelSource failed by ${ex.message}")
        }
    }

    override suspend fun deleteTVChannelSource(tvChannelSource: TVChannelSource) {
        try {
            withContext(Dispatchers.IO) {
                val sourceId = tvChannelSource.id
                dao.deleteTVChannelsBySource(sourceId)
                dao.deleteTVChannelGroupsBySource(sourceId)
                dao.deleteTVChannelSource(tvChannelSource)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("deleteTVChannelSource failed by ${ex.message}")
        }
    }

    override suspend fun deleteTVChannelSourceById(id: Long) {
        try {
            withContext(Dispatchers.IO) {
                dao.deleteTVChannelsBySource(id)
                dao.deleteTVChannelGroupsBySource(id)
                dao.deleteTVChannelSourceById(id)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("deleteTVChannelSourceById failed by ${ex.message}")
        }
    }

    override suspend fun getChannelCountBySource(sourceId: Long): Int {
        return try {
            withContext(Dispatchers.IO) {
                dao.getChannelCountBySource(sourceId)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("getChannelCountBySource failed by ${ex.message}")
            0
        }
    }

    override suspend fun getGroupCountBySource(sourceId: Long): Int {
        return try {
            withContext(Dispatchers.IO) {
                dao.getGroupCountBySource(sourceId)
            }
        } catch (ex: Exception) {
            Timber.tag("Kai").d("getGroupCountBySource failed by ${ex.message}")
            0
        }
    }

    override suspend fun getTvChanelByName(name: String): TVChannelSource? {
        return try {
            withContext(Dispatchers.IO) {
                dao.getTVChannelSourceByName(name)
            }
        } catch (ex: Exception) {
            null
        }
    }
}

package com.anrstudio.template.data.iptv.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.anrstudio.template.data.iptv.model.TVChannel
import com.anrstudio.template.data.iptv.model.TVChannelGroup
import com.anrstudio.template.data.iptv.model.TVChannelSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query(
        """
        SELECT * FROM tb_tv_channel
        WHERE id IN (
            SELECT MAX(id) FROM tb_tv_channel
            GROUP BY url
        )
        ORDER BY name
    """
    )
    fun getAllTVChannel(): Flow<List<TVChannel>>

    @Query("SELECT * FROM tb_tv_channel WHERE sourceId = :sourceId")
    fun getTVChannelsBySource(sourceId: Long): Flow<List<TVChannel>>

    @Query(
        """
        SELECT * FROM tb_tv_channel
        WHERE groupId = :groupId
        AND id IN (
            SELECT MAX(id) FROM tb_tv_channel
            WHERE groupId = :groupId
            GROUP BY url
        )
        ORDER BY name
    """
    )
    fun getTVChannelsByGroup(groupId: String): Flow<List<TVChannel>>

    @Query(
        """
        SELECT * FROM tb_tv_channel
        WHERE isFavorite = 1
        AND id IN (
            SELECT MAX(id) FROM tb_tv_channel
            WHERE isFavorite = 1
            GROUP BY url
        )
        ORDER BY connectedTime DESC
    """
    )
    fun getFavoriteTVChannels(): Flow<List<TVChannel>>

    @Query(
        """
        SELECT * FROM tb_tv_channel
        WHERE connectedTime != ''
        AND id IN (
            SELECT MAX(id) FROM tb_tv_channel
            WHERE connectedTime != ''
            GROUP BY url
        )
        ORDER BY connectedTime DESC
    """
    )
    fun getRecentTVChannels(): Flow<List<TVChannel>>

    @Query(
        """
        SELECT * FROM tb_tv_channel
        WHERE (name LIKE :query OR groupTitle LIKE :query)
        AND id IN (
            SELECT MAX(id) FROM tb_tv_channel
            WHERE (name LIKE :query OR groupTitle LIKE :query)
            GROUP BY url
        )
        ORDER BY name
    """
    )
    fun searchTVChannels(query: String): Flow<List<TVChannel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTVChannel(tvChannel: TVChannel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTVChannels(tvChannels: List<TVChannel>)

    @Update
    fun updateTVChannel(tvChannel: TVChannel)

    @Delete
    fun deleteTVChannel(tvChannel: TVChannel)

    @Query("DELETE FROM tb_tv_channel WHERE sourceId = :sourceId")
    fun deleteTVChannelsBySource(sourceId: Long)

    @Query("DELETE FROM tb_tv_channel WHERE groupId = :groupId")
    fun deleteTVChannelsByGroup(groupId: String)

    @Query("SELECT * FROM tb_tv_channel_group")
    fun getAllTVChannelGroup(): Flow<List<TVChannelGroup>>

    @Query("SELECT * FROM tb_tv_channel_group WHERE sourceId = :sourceId")
    fun getTVChannelGroupsBySource(sourceId: Long): Flow<List<TVChannelGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTVChannelGroup(tvChannelGroup: TVChannelGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTVChannelGroups(tvChannelGroups: List<TVChannelGroup>)

    @Update
    fun updateTVChannelGroup(tvChannelGroup: TVChannelGroup)

    @Delete
    fun deleteTVChannelGroup(tvChannelGroup: TVChannelGroup)

    @Query("DELETE FROM tb_tv_channel_group WHERE sourceId = :sourceId")
    fun deleteTVChannelGroupsBySource(sourceId: Long)

    @Query("SELECT * FROM tb_tv_channel_source ORDER BY importDate DESC")
    fun getAllTVChannelSource(): Flow<List<TVChannelSource>>

    @Query("SELECT name FROM tb_tv_channel_source ORDER BY name")
    fun getAllTVChannelSourceNames(): Flow<List<String>>

    @Query("SELECT * FROM tb_tv_channel_source WHERE id = :id")
    fun getTVChannelSourceById(id: Long): Flow<TVChannelSource?>

    @Query("SELECT * FROM tb_tv_channel_source WHERE sourceType = :sourceType ORDER BY importDate DESC")
    fun getTVChannelSourcesByType(sourceType: String): Flow<List<TVChannelSource>>

    @Query("SELECT * FROM tb_tv_channel_source WHERE isActive = 1 ORDER BY importDate DESC")
    fun getActiveTVChannelSources(): Flow<List<TVChannelSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTVChannelSource(tvChannelSource: TVChannelSource): Long

    @Query("SELECT * FROM tb_tv_channel_source WHERE name = :name LIMIT 1")
    fun getTVChannelSourceByName(name: String): TVChannelSource

    @Update
    fun updateTVChannelSource(tvChannelSource: TVChannelSource)

    @Delete
    fun deleteTVChannelSource(tvChannelSource: TVChannelSource)

    @Query("DELETE FROM tb_tv_channel_source WHERE id = :id")
    fun deleteTVChannelSourceById(id: Long)

    @Query("SELECT COUNT(*) FROM tb_tv_channel WHERE sourceId = :sourceId")
    fun getChannelCountBySource(sourceId: Long): Int

    @Query("SELECT COUNT(*) FROM tb_tv_channel_group WHERE sourceId = :sourceId")
    fun getGroupCountBySource(sourceId: Long): Int
}

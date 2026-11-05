package com.anrstudio.template.data.iptv.database.manager

import android.util.Log
import com.anrstudio.template.data.iptv.database.repository.ChannelDaoRepository
import com.anrstudio.template.data.iptv.model.ChannelOnline
import com.anrstudio.template.data.iptv.model.TVChannel
import com.anrstudio.template.data.iptv.model.TVChannelGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChannelManager @Inject constructor(
    private val channelDaoRepository: ChannelDaoRepository
) {

    companion object {

        private const val TAG = "Kai"

        const val UNKNOW_FROUP_NAME = "Unknow"
    }

    private val okHttpClient =
        OkHttpClient.Builder().connectTimeout(3, TimeUnit.MINUTES).writeTimeout(3, TimeUnit.MINUTES)
            .readTimeout(3, TimeUnit.MINUTES).callTimeout(3, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true).build()

    private val listTVChannel = arrayListOf<TVChannel>()

    private val listTVChannelGroup = arrayListOf<TVChannelGroup>()

    fun getListTvChannel(): ArrayList<TVChannel> {
        val data = CopyOnWriteArrayList<TVChannel>()
        data.addAll(listTVChannel)
        return ArrayList(data)
    }

    suspend fun queryTVChannel(isQueryAgain: Boolean) {
        if (listTVChannel.isEmpty() || isQueryAgain) {
            listTVChannel.clear()
            listTVChannel.addAll(channelDaoRepository.getAllTVChannel().first())
        }
    }

    suspend fun queryTVChannelGroup(isQueryAgain: Boolean) {
        if (listTVChannelGroup.isEmpty() || isQueryAgain) {
            val data = CopyOnWriteArrayList<TVChannelGroup>()
            data.addAll(channelDaoRepository.getAllTVChannelGroup().first())
            data.forEach { group ->
                group.channelNumber = listTVChannel.count { it.groupId == group.name }
            }
            listTVChannelGroup.clear()
            listTVChannelGroup.addAll(ArrayList(data))
        }
    }

    suspend fun queryData(isQueryAgain: Boolean = false) {
        queryTVChannel(isQueryAgain)
        queryTVChannelGroup(isQueryAgain)
    }

    suspend fun getPlayListFromUrl(url: String) {
        val request = Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()
        val response = okHttpClient.newCall(request).execute()
        val channelsOnline = if (response.isSuccessful) {
            val content = response.body?.string() ?: ""
            parseContent(content)
        } else {
            Log.e(TAG, "Failed to fetch playlist: ${response.code}")
            emptyList()
        }
        if (channelsOnline.isNotEmpty()) {
            val data = CopyOnWriteArrayList<ChannelOnline>()
            data.addAll(channelsOnline)

            // Create group
            val groupName = data.map { it.groupTitle }.distinct()
            if (groupName.isNotEmpty()) {
                groupName.forEach { name ->
                    val exists = listTVChannelGroup.find { it.name == name }
                    if (exists == null) {
                        val tvChannelGroup = TVChannelGroup(
                            name = name,
                            displayName = name,
                            channelNumber = 0,
                            isFavorite = false
                        )
                        listTVChannelGroup.add(tvChannelGroup)
                        channelDaoRepository.insertTVChannelGroup(tvChannelGroup)
                    }
                }
            }

            data.forEach { channelOnline ->
                val groupOfChannel = listTVChannelGroup.find { it.name == channelOnline.groupTitle }
                val tvChannel = TVChannel.convertFromOnline(channelOnline, groupOfChannel)
                channelDaoRepository.insertTVChannel(tvChannel)
            }
        }
        queryData(true)
    }

    private fun parseContent(content: String): List<ChannelOnline> {
        if (content.isBlank()) return emptyList()
        try {
            val channels = mutableListOf<ChannelOnline>()
            var currentName: String? = null
            var currentGroup: String? = null
            var currentLogo: String? = null
            var currentTvgId: String? = null
            content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
                when {
                    line.startsWith("#EXTINF:") -> {
                        val infoPart = line.substringAfter("#EXTINF:").substringBeforeLast(",")
                        currentName = line.substringAfterLast(",").trim().ifEmpty {
                            Regex(",(.+)$").find(line)?.groupValues?.getOrNull(1)?.trim()
                        }
                        currentTvgId =
                            Regex("""tvg-id="([^"]+)"""").find(infoPart)?.groupValues?.get(1)
                        currentLogo =
                            Regex("""tvg-logo="([^"]+)"""").find(infoPart)?.groupValues?.get(1)
                        currentGroup =
                            Regex("""group-title="([^"]+)"""").find(infoPart)?.groupValues?.get(1)
                    }

                    !line.startsWith("#") && currentName != null -> {
                        channels += ChannelOnline(
                            name = currentName,
                            url = line,
                            groupTitle = currentGroup ?: UNKNOW_FROUP_NAME,
                            logo = currentLogo.orEmpty(),
                            tvgId = currentTvgId.orEmpty()
                        )
                        currentName = null
                        currentGroup = null
                        currentLogo = null
                        currentTvgId = null
                    }
                }
            }
            Log.d(TAG, "Parsed success: ${channels.size} channels")
            return channels
        } catch (ex: Exception) {
            Log.e(TAG, "parseContent failed by: ${ex.message}")
            return emptyList()
        }
    }

    suspend fun getPlayListForGroupById(groupId: String): Flow<List<TVChannel>> {
        val channels = channelDaoRepository.getTVChannelsByGroup(groupId)
        return channels
    }

    suspend fun getPlayListForSourceById(sourceId: Long): Flow<List<TVChannel>> {
        val channels = channelDaoRepository.getTVChannelsBySource(sourceId)
        return channels
    }

    suspend fun getFavoriteChannels(): Flow<List<TVChannel>> {
        return channelDaoRepository.getFavoriteTVChannels()
    }

    suspend fun getRecentChannels(): Flow<List<TVChannel>> {
        return channelDaoRepository.getRecentTVChannels()
    }

    suspend fun getAllChannels(): Flow<List<TVChannel>> {
        return channelDaoRepository.getAllTVChannel()
    }

    suspend fun getAllTVChannelSourceNames(): Flow<List<String>> {
        return channelDaoRepository.getAllTVChannelSourceNames()
    }

}

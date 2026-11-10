package com.hyper.castwave.ui.component.iptv.addchannel.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.hyper.castwave.data.iptv.database.manager.ChannelDataManager
import com.hyper.castwave.data.iptv.model.ChannelOnline
import com.hyper.castwave.data.iptv.model.DataState
import com.hyper.castwave.data.iptv.model.TVChannel
import com.hyper.castwave.data.iptv.model.TVChannelGroup
import com.hyper.castwave.data.iptv.model.TVChannelSource
import com.hyper.castwave.data.iptv.model.TVChannelSourceType
import com.hyper.castwave.ui.bases.BaseViewModel
import com.hyper.castwave.ui.component.iptv.addchannel.helper.FileHelper
import com.hyper.castwave.utils.M3UParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

@HiltViewModel
class AddChannelViewModel @Inject constructor(
    private val channelDataManager: ChannelDataManager
) : BaseViewModel() {
    val allChannelNames: Flow<List<String>> = channelDataManager.getAllTVChannelSourceNames()

    private val _importState = MutableStateFlow<DataState<ImportResult>>(DataState.None)
    val importState: StateFlow<DataState<ImportResult>> = _importState

    fun importFromUrl(playlistName: String, url: String) {
        bgScope.launch {
            _importState.update { DataState.Loading }
            try {
                val channels = M3UParser.parseM3U(url)
                if (channels.isEmpty()) {
                    _importState.update { DataState.Error("No channels found in playlist") }
                    return@launch
                }
                processImport(playlistName, url, channels, TVChannelSourceType.URL)
            } catch (e: Exception) {
                _importState.update { DataState.Error(e.message ?: "Failed to import from URL") }
            }
        }
    }

    fun importFromStream(name: String, url: String) {
        bgScope.launch {
            _importState.update { DataState.Loading }
            try {
                val channel = ChannelOnline(
                    name = name,
                    url = url,
                    groupTitle = "Stream",
                    logo = "",
                    tvgId = ""
                )
                processImport(name, url, listOf(channel), TVChannelSourceType.STREAM)
            } catch (e: Exception) {
                _importState.update { DataState.Error(e.message ?: "Failed to import stream") }
            }
        }
    }

    fun importFromFile(context: Context, playlistName: String, fileUri: Uri) {
        bgScope.launch {
            _importState.update { DataState.Loading }
            try {
                val content = readFileContent(context, fileUri)
                if (content.isEmpty()) {
                    _importState.update { DataState.Error("Failed to read file") }
                    return@launch
                }

                val channels = parseM3UContent(content)
                if (channels.isEmpty()) {
                    _importState.update { DataState.Error("No channels found in file") }
                    return@launch
                }

                val filePath = FileHelper.getFileName(context, fileUri)
                processImport(playlistName, filePath, channels, TVChannelSourceType.FILE)
            } catch (e: Exception) {
                _importState.update { DataState.Error(e.message ?: "Failed to import from file") }
            }
        }
    }

    fun importFromDevice(context: Context, playlistName: String, videoUris: List<Uri>) {
        bgScope.launch {
            _importState.update { DataState.Loading }
            try {
                if (videoUris.isEmpty()) {
                    _importState.update { DataState.Error("No videos selected") }
                    return@launch
                }

                val channels = videoUris.mapIndexed { index, uri ->
                    val videoName = getVideoNameFromUri(context, uri) ?: "Video ${index + 1}"
                    ChannelOnline(
                        name = videoName,
                        url = uri.toString(),
                        groupTitle = "Device",
                        logo = "",
                        tvgId = ""
                    )
                }

                processImport(playlistName, "device://", channels, TVChannelSourceType.DEVICE)
            } catch (e: Exception) {
                _importState.update { DataState.Error(e.message ?: "Failed to import from device") }
            }
        }
    }

    private fun getVideoNameFromUri(context: Context, uri: Uri): String? {
        return try {
            var name: String? = null
            
            // First, try to get name from ContentResolver (works for both MediaStore and content:// URIs)
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // Try DISPLAY_NAME first
                    val displayNameIndex = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    if (displayNameIndex >= 0) {
                        name = cursor.getString(displayNameIndex)
                    }
                    
                    // If no DISPLAY_NAME, try OpenableColumns.DISPLAY_NAME
                    if (name.isNullOrEmpty()) {
                        val openableNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (openableNameIndex >= 0) {
                            name = cursor.getString(openableNameIndex)
                        }
                    }
                    
                    // If still no name, try TITLE
                    if (name.isNullOrEmpty()) {
                        val titleIndex = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                        if (titleIndex >= 0) {
                            name = cursor.getString(titleIndex)
                        }
                    }
                }
            }

            if (name.isNullOrEmpty()) {
                val path = uri.path
                if (path != null) {
                    val fileName = path.substringAfterLast("/")
                    if (fileName.isNotEmpty()) {
                        name = fileName
                    }
                }
            }

            name?.let {
                if (it.contains(".")) {
                    it.substringBeforeLast(".")
                } else {
                    it
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun resetImportState() {
        _importState.update { DataState.None }
    }

    private suspend fun processImport(
        name: String,
        sourceUrl: String,
        channels: List<ChannelOnline>,
        sourceType: TVChannelSourceType
    ) {
        var existsName = channelDataManager.getTVChannelSourceByName(name)
        var index = 0
        var newName = name
        while (existsName != null) {
            index += 1
            newName = "$name ($index)"
            existsName = channelDataManager.getTVChannelSourceByName(newName)
        }
        val source = createSource(newName, sourceUrl, sourceType)
        val sourceId = channelDataManager.insertTVChannelSource(source)

        val result = saveChannelsAndGroups(channels, sourceId)

        val updatedSource = source.copy(
            id = sourceId,
            channelCount = result.channelCount,
            groupCount = result.groupCount
        )
        channelDataManager.updateTVChannelSource(updatedSource)

        _importState.update {
            DataState.Success(
                listOf(
                    ImportResult(
                        sourceId = sourceId,
                        channelCount = result.channelCount,
                        groupCount = result.groupCount
                    )
                )
            )
        }
    }

    private fun createSource(
        name: String,
        sourceUrl: String,
        sourceType: TVChannelSourceType
    ): TVChannelSource {
        return TVChannelSource(
            name = name,
            sourceUrl = sourceUrl,
            sourceType = sourceType.value,
            importDate = System.currentTimeMillis(),
            channelCount = 0,
            groupCount = 0,
            isActive = true,
            lastUpdateTime = System.currentTimeMillis(),
            description = ""
        )
    }


    private val _tvChannelsFlow = MutableStateFlow<List<TVChannel>>(emptyList())
    val tvChannelsFlow: StateFlow<List<TVChannel>> = _tvChannelsFlow

    private suspend fun saveChannelsAndGroups(
        channels: List<ChannelOnline>,
        sourceId: Long
    ): SaveResult {
        val groupNames = channels.map { it.groupTitle }.distinct()
        val allExistingGroups = channelDataManager.getAllTVChannelGroup().first()
        val existingGroupsForSource = channelDataManager.getTVChannelGroupsBySource(sourceId).first()

        val groups = CopyOnWriteArrayList<TVChannelGroup>()
        groupNames.forEach { groupName ->
            var existingGroup = existingGroupsForSource.find {
                it.displayName == groupName || it.name == groupName
            }

            if (existingGroup == null) {
                val globalGroup = allExistingGroups.find { it.name == groupName }
                val uniqueGroupName = if (globalGroup == null) {
                    groupName
                } else {
                    "${groupName}_source_$sourceId"
                }

                val newGroup = TVChannelGroup(
                    name = uniqueGroupName,
                    displayName = groupName,
                    channelNumber = 0,
                    isFavorite = false,
                    sourceId = sourceId
                )
                channelDataManager.insertTVChannelGroup(newGroup)
                groups.add(newGroup)
            } else {
                groups.add(existingGroup)
            }
        }

        val tvChannels = channels.map { channelOnline ->
            val group = groups.find {
                it.displayName == channelOnline.groupTitle ||
                it.name == channelOnline.groupTitle ||
                it.name.contains(channelOnline.groupTitle)
            }
            TVChannel.convertFromOnline(channelOnline, group, sourceId)
        }

        channelDataManager.insertTVChannels(tvChannels)

        _tvChannelsFlow.emit(tvChannels)

        return SaveResult(
            channelCount = tvChannels.size,
            groupCount = groups.size
        )
    }

    private fun parseM3UContent(content: String): List<ChannelOnline> {
        val channelOnlines = mutableListOf<ChannelOnline>()
        val lines = content.lines()

        var currentName: String? = null
        var currentUrl: String? = null
        var currentGroupTitle: String? = null
        var currentLogo: String? = null
        var currentTvgId: String? = null

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("#EXTINF:")) {
                currentName = null
                currentUrl = null
                currentGroupTitle = null
                currentLogo = null
                currentTvgId = null

                val namePart = trimmed.substringAfterLast(",")
                if (namePart.isNotEmpty()) {
                    currentName = namePart.trim()
                }

                val attributesPart = trimmed.substringAfter("#EXTINF:").substringBeforeLast(",")

                currentTvgId = Regex("tvg-id=\"([^\"]+)\"").find(attributesPart)?.groupValues?.get(1)
                currentLogo = Regex("tvg-logo=\"([^\"]+)\"").find(attributesPart)?.groupValues?.get(1)
                // If multiple categories separated by ';', only take the first one
                currentGroupTitle = Regex("group-title=\"([^\"]+)\"").find(attributesPart)?.groupValues?.get(1)?.split(";")?.firstOrNull()?.trim()

                if (currentName.isNullOrEmpty()) {
                    currentName = Regex(",(.+)$").find(trimmed)?.groupValues?.get(1)?.trim()
                }
            } else if (!trimmed.startsWith("#") && trimmed.isNotEmpty() && currentName != null) {
                currentUrl = trimmed

                if (currentName.isNotEmpty() && currentUrl.isNotEmpty()) {
                    channelOnlines.add(
                        ChannelOnline(
                            name = currentName,
                            url = currentUrl,
                            groupTitle = currentGroupTitle ?: "",
                            logo = currentLogo ?: "",
                            tvgId = currentTvgId ?: ""
                        )
                    )
                }

                currentName = null
                currentUrl = null
                currentGroupTitle = null
                currentLogo = null
                currentTvgId = null
            }
        }

        return channelOnlines
    }

    private suspend fun readFileContent(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    data class ImportResult(
        val sourceId: Long,
        val channelCount: Int,
        val groupCount: Int
    )

    private data class SaveResult(
        val channelCount: Int,
        val groupCount: Int
    )
}

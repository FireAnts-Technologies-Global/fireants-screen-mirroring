package com.anrstudio.template.utils

import android.util.Log
import com.anrstudio.template.data.iptv.model.ChannelOnline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object M3UParser {

    private const val TAG = "M3UParser"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun parseM3U(playlistUrl: String): List<ChannelOnline> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(playlistUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val content = response.body?.string() ?: return@withContext emptyList()
                parseM3UContent(content)
            } else {
                Log.e(TAG, "Failed to fetch playlist: ${response.code}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M3U: ${e.message}", e)
            emptyList()
        }
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
                currentGroupTitle = Regex("group-title=\"([^\"]+)\"")
                    .find(attributesPart)
                    ?.groupValues
                    ?.get(1)
                    ?.split(";")
                    ?.firstOrNull()
                    ?.trim()

                if (currentName.isNullOrEmpty()) {
                    currentName = Regex(",(.+)$").find(trimmed)?.groupValues?.get(1)?.trim()
                }
            } else if (!trimmed.startsWith("#") && trimmed.isNotEmpty() && currentName != null) {
                currentUrl = trimmed
                if (!currentName.isNullOrEmpty() && !currentUrl.isNullOrEmpty()) {
                    channelOnlines.add(
                        ChannelOnline(
                            name = currentName,
                            url = currentUrl,
                            groupTitle = currentGroupTitle ?: "",
                            logo = currentLogo ?: "",
                            tvgId = currentTvgId ?: "",
                        ),
                    )
                }
                currentName = null
                currentUrl = null
                currentGroupTitle = null
                currentLogo = null
                currentTvgId = null
            }
        }

        Log.d(TAG, "Parsed ${channelOnlines.size} channels")
        return channelOnlines
    }
}

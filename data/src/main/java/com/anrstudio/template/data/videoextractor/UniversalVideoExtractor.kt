package com.anrstudio.template.data.videoextractor

import android.content.Context
import android.util.SparseArray
import android.webkit.WebView
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.anrstudio.template.domain.model.ExtractedVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import vimeoextractor.OnVimeoExtractionListener
import vimeoextractor.VimeoExtractor
import vimeoextractor.VimeoVideo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class UniversalVideoExtractor(private val context: Context) {

    companion object {
        private const val TAG = "UniversalVideoExtractor"

        private val VIDEO_EXTENSIONS =
            arrayOf("mp4", "webm", "ogg", "ogv", "avi", "mov", "wmv", "flv", "m4v")

        private val STREAM_EXTENSIONS = arrayOf("m3u8", "m3u", "mpd", "ts")
    }

    interface VideoExtractionCallback {
        fun onVideoFound(videoDto: ExtractedVideo)
        fun onExtractionComplete(videos: List<ExtractedVideo>)
        fun onError(error: String)
        fun onProgress(message: String)
    }

    private val extractedVideos = mutableListOf<ExtractedVideo>()
    private var callback: VideoExtractionCallback? = null

    fun extractVideo(url: String, callback: VideoExtractionCallback) {
        this.callback = callback
        extractedVideos.clear()

        callback.onProgress("Đang phân tích URL...")

        when {
            isYouTubeUrl(url) -> {
                extractYouTubeVideo(url)
            }

            isVimeoUrl(url) -> {
                extractVimeoVideo(url)
            }

            isTikTokUrl(url) -> {
                extractTikTokVideo(url)
            }

            isTwitchUrl(url) -> {
                extractTwitchVideo(url)
            }

            isIPTVUrl(url) -> {
                extractIPTVStream(url)
            }

            isDirectVideoUrl(url) -> {
                extractDirectVideo(url)
            }

            else -> {
                extractGenericVideo(url)
            }
        }
    }

    fun extractFromWebView(webView: WebView, callback: VideoExtractionCallback) {
        this.callback = callback
        extractedVideos.clear()

        val url = webView.url ?: return
        val title = webView.title ?: "Unknown"

        val script = """
            (function() {
                var videos = [];
                document.querySelectorAll('video').forEach(function(video) {
                    if (video.src || video.currentSrc) {
                        videos.push({ url: video.src || video.currentSrc, title: '$title', type: 'video' });
                    }
                });
                document.querySelectorAll('iframe').forEach(function(iframe) {
                    var src = iframe.src;
                    if (src && (src.includes('youtube') || src.includes('vimeo') || src.includes('tiktok'))) {
                        videos.push({ url: src, title: '$title', type: 'iframe' });
                    }
                });
                document.querySelectorAll('a').forEach(function(link) {
                    var href = link.href;
                    if (href && (href.includes('.mp4') || href.includes('.m3u8') || href.includes('.webm'))) {
                        videos.push({ url: href, title: link.textContent || '$title', type: 'link' });
                    }
                });
                return JSON.stringify(videos);
            })();
        """

        webView.evaluateJavascript(script) { result ->
            try {
                if (result != "null" && result != "\"[]\"") {
                    val cleanResult = result.replace("\\\"", "\"").removeSurrounding("\"")
                    val jsonArray = JSONArray(cleanResult)

                    for (i in 0 until jsonArray.length()) {
                        val videoObj = jsonArray.getJSONObject(i)
                        val videoUrl = videoObj.getString("url")

                        extractVideo(videoUrl, object : VideoExtractionCallback {
                            override fun onVideoFound(videoDto: ExtractedVideo) {
                                callback.onVideoFound(videoDto)
                            }

                            override fun onExtractionComplete(videos: List<ExtractedVideo>) {}
                            override fun onError(error: String) {}
                            override fun onProgress(message: String) {}
                        })
                    }
                }

                extractVideo(url, callback)
            } catch (e: Exception) {
                extractVideo(url, callback)
            }
        }
    }

    private fun extractYouTubeVideo(url: String) {
        callback?.onProgress("Đang trích xuất video YouTube...")

        val videoId = extractYouTubeVideoId(url)
        if (videoId.isEmpty()) {
            callback?.onError("Không thể lấy YouTube video ID")
            return
        }

        val youTubeExtractor = object : YouTubeExtractor(context) {
            override fun onExtractionComplete(
                sparseArray: SparseArray<YtFile>?,
                videoMeta: VideoMeta?
            ) {
                if (sparseArray != null && videoMeta != null) {
                    for (i in 0 until sparseArray.size()) {
                        val ytFile = sparseArray[sparseArray.keyAt(i)]

                        if (isValidYouTubeFormat(ytFile)) {
                            val youtubeDto = ExtractedVideo(
                                videoMeta.title,
                                videoMeta.hqImageUrl,
                                ytFile.url,
                                "${ytFile.format.height}p",
                                ytFile.format.ext,
                                videoMeta.videoLength
                            )

                            extractedVideos.add(youtubeDto)
                            callback?.onVideoFound(youtubeDto)
                        }
                    }
                    callback?.onExtractionComplete(extractedVideos)
                } else {
                    callback?.onError("Không thể trích xuất video YouTube")
                }
            }
        }

        youTubeExtractor.extract("http://youtube.com/watch?v=$videoId")
    }

    private fun extractVimeoVideo(url: String) {
        callback?.onProgress("Đang trích xuất video Vimeo...")

        val videoId = extractVimeoVideoId(url)

        if (videoId.isEmpty()) {
            callback?.onError("Không thể lấy Vimeo video ID")
            return
        }

        VimeoExtractor.getInstance().fetchVideoWithIdentifier(
            videoId, null,
            object : OnVimeoExtractionListener {
                override fun onSuccess(vimeoVideo: VimeoVideo) {
                    val qualityList = arrayOf("240p", "360p", "480p", "720p", "1080p")
                    val thumbList = arrayOf("640", "940", "1280", "base")

                    var foundVideo = false
                    outerLoop@ for (quality in qualityList) {
                        if (vimeoVideo.streams.containsKey(quality)) {
                            for (thumbSize in thumbList) {
                                if (vimeoVideo.thumbs.containsKey(thumbSize)) {
                                    val youtubeDto = ExtractedVideo(
                                        vimeoVideo.title,
                                        vimeoVideo.thumbs[thumbSize] as String,
                                        vimeoVideo.streams[quality] as String,
                                        quality,
                                        "mp4",
                                        vimeoVideo.duration.toLong()
                                    )

                                    extractedVideos.add(youtubeDto)
                                    callback?.onVideoFound(youtubeDto)
                                    foundVideo = true
                                    break@outerLoop
                                }
                            }
                        }
                    }

                    if (!foundVideo) {
                        callback?.onError("Không tìm thấy stream video phù hợp")
                    } else {
                        callback?.onExtractionComplete(extractedVideos)
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                    callback?.onError("Lỗi trích xuất Vimeo: ${throwable?.message}")
                }
            }
        )
    }

    private fun extractTikTokVideo(url: String) {
        callback?.onProgress("Đang trích xuất video TikTok...")

        if (url.startsWith("snssdk") || url.contains("aweme/detail")) {
            callback?.onError("URL TikTok này không được hỗ trợ")
            return
        }

        if (url.contains("/explore") || url.contains("/foryou") ||
            url.matches(Regex(".*tiktok\\.com/[a-z]{2}-[A-Z]{2}/?.*")) ||
            url.matches(Regex(".*tiktok\\.com/?$"))
        ) {

            callback?.onProgress("Đang tìm video trong trang TikTok...")
            extractGenericVideo(url)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val htmlContent = fetchWebContent(url)

                if (htmlContent.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback?.onError("Không thể tải nội dung từ TikTok")
                    }
                    return@launch
                }

                val videoUrls = extractTikTokVideoUrls(htmlContent)

                if (videoUrls.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        videoUrls.forEach { videoUrl ->
                            val youtubeDto = ExtractedVideo(
                                "TikTok Video",
                                "",
                                videoUrl,
                                "Auto",
                                "mp4",
                                0
                            )

                            extractedVideos.add(youtubeDto)
                            callback?.onVideoFound(youtubeDto)
                        }
                        callback?.onExtractionComplete(extractedVideos)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback?.onError("Không thể trích xuất video TikTok")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onError("Lỗi TikTok: ${e.message}")
                }
            }
        }
    }

    private fun extractTwitchVideo(url: String) {
        callback?.onProgress("Đang trích xuất stream Twitch...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channelName = extractTwitchChannelName(url)

                if (channelName.isNotEmpty()) {
                    val streamUrls = getTwitchStreamUrls(channelName)

                    withContext(Dispatchers.Main) {
                        if (streamUrls.isNotEmpty()) {
                            streamUrls.forEach { (quality, streamUrl) ->
                                val youtubeDto = ExtractedVideo(
                                    "Twitch: $channelName",
                                    "",
                                    streamUrl,
                                    quality,
                                    "m3u8",
                                    0
                                )

                                extractedVideos.add(youtubeDto)
                                callback?.onVideoFound(youtubeDto)
                            }
                            callback?.onExtractionComplete(extractedVideos)
                        } else {
                            callback?.onError("Không tìm thấy stream Twitch")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback?.onError("Không thể lấy tên channel Twitch")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onError("Lỗi Twitch: ${e.message}")
                }
            }
        }
    }

    private fun extractIPTVStream(url: String) {
        callback?.onProgress("Đang trích xuất IPTV stream...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when {
                    url.endsWith(".m3u") || url.endsWith(".m3u8") -> {
                        parseM3U8Playlist(url)
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            val youtubeDto = ExtractedVideo(
                                "IPTV Stream",
                                "",
                                url,
                                "Auto",
                                "m3u8",
                                0
                            )

                            extractedVideos.add(youtubeDto)
                            callback?.onVideoFound(youtubeDto)
                            callback?.onExtractionComplete(extractedVideos)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onError("Lỗi IPTV: ${e.message}")
                }
            }
        }
    }

    private fun extractDirectVideo(url: String) {
        callback?.onProgress("Đang xử lý video trực tiếp...")

        val extension = getFileExtension(url)
        val youtubeDto = ExtractedVideo(
            "Direct Video",
            "",
            url,
            "Auto",
            extension,
            0
        )

        extractedVideos.add(youtubeDto)
        callback?.onVideoFound(youtubeDto)
        callback?.onExtractionComplete(extractedVideos)
    }

    private fun extractGenericVideo(url: String) {
        callback?.onProgress("Đang tìm kiếm video trong trang web...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val htmlContent = fetchWebContent(url)

                if (htmlContent.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback?.onError("Không thể tải nội dung từ trang web")
                    }
                    return@launch
                }

                val videoUrls = extractVideoUrlsFromHtml(htmlContent, url)

                withContext(Dispatchers.Main) {
                    if (videoUrls.isNotEmpty()) {
                        videoUrls.forEach { videoUrl ->
                            val youtubeDto = ExtractedVideo(
                                "Web Video",
                                "",
                                videoUrl,
                                "Auto",
                                getFileExtension(videoUrl),
                                0
                            )

                            extractedVideos.add(youtubeDto)
                            callback?.onVideoFound(youtubeDto)
                        }
                        callback?.onExtractionComplete(extractedVideos)
                    } else {
                        callback?.onError("Không tìm thấy video trong trang web")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback?.onError("Lỗi generic extraction: ${e.message}")
                }
            }
        }
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be") || url.contains("m.youtube.com")
    }

    private fun isVimeoUrl(url: String): Boolean {
        return url.contains("vimeo.com")
    }

    private fun isTikTokUrl(url: String): Boolean {
        return url.contains("tiktok.com") && !url.startsWith("snssdk")
    }

    private fun isTwitchUrl(url: String): Boolean {
        return url.contains("twitch.tv")
    }

    private fun isIPTVUrl(url: String): Boolean {
        return url.endsWith(".m3u") || url.endsWith(".m3u8") || url.contains("iptv")
    }

    private fun isDirectVideoUrl(url: String): Boolean {
        val extension = getFileExtension(url).lowercase()
        return VIDEO_EXTENSIONS.contains(extension) || STREAM_EXTENSIONS.contains(extension)
    }

    private fun extractYouTubeVideoId(url: String): String {
        val patterns = arrayOf(
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/|m\\.youtube\\.com/watch\\?v=)([\\w-]+)",
            "youtube\\.com/embed/([\\w-]+)",
            "youtube\\.com/v/([\\w-]+)"
        )

        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(url)
            if (matcher.find()) {
                return matcher.group(1) ?: ""
            }
        }
        return ""
    }

    private fun extractVimeoVideoId(url: String): String {
        val patterns = arrayOf(
            "vimeo\\.com/(\\d+)",
            "vimeo\\.com/channels/[^/]+/(\\d+)",
            "vimeo\\.com/groups/[^/]+/videos/(\\d+)",
            "vimeo\\.com/video/(\\d+)"
        )

        for (pattern in patterns) {
            val matcher = Pattern.compile(pattern).matcher(url)
            if (matcher.find()) {
                return matcher.group(1) ?: ""
            }
        }

        return ""
    }

    private fun extractTwitchChannelName(url: String): String {
        val pattern = "twitch\\.tv/([\\w-]+)"
        val matcher = Pattern.compile(pattern).matcher(url)
        return if (matcher.find()) matcher.group(1) ?: "" else ""
    }

    private fun isValidYouTubeFormat(ytFile: YtFile): Boolean {
        return (ytFile.format.ext == "mp4" || ytFile.format.ext == "m3u" || ytFile.format.ext == "m3u8") &&
                (ytFile.format.height in arrayOf(144, 240, 360, 480, 720, 1080)) &&
                ytFile.format.audioBitrate != -1
    }

    private fun getFileExtension(url: String): String {
        return try {
            val cleanUrl = url.split("?")[0]
            cleanUrl.substringAfterLast(".", "mp4")
        } catch (e: Exception) {
            "mp4"
        }
    }

    private suspend fun fetchWebContent(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val content = reader.readText()
                    reader.close()
                    content
                } else {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun extractTikTokVideoUrls(html: String): List<String> {
        val videoUrls = mutableListOf<String>()

        val patterns = arrayOf(
            "\"playAddr\":\"([^\"]+)\"",
            "\"downloadAddr\":\"([^\"]+)\"",
            "\"videoUrl\":\"([^\"]+)\"",
            "\"url\":\"([^\"]*\\.mp4[^\"]*)\"",
            "src=\"([^\"]*\\.mp4[^\"]*)\"",
            "https://[^\\s\"']*\\.mp4[^\\s\"']*",
            "\"playUrl\":\"([^\"]+)\"",
            "\"videoUrlNoWaterMark\":\"([^\"]+)\"",
            "v16-webapp[^\\s\"']*\\.mp4[^\\s\"']*",
            "p16-sign[^\\s\"']*\\.mp4[^\\s\"']*"
        )

        patterns.forEachIndexed { index, pattern ->
            val matcher = Pattern.compile(pattern).matcher(html)
            while (matcher.find()) {
                val url =
                    matcher.group(1)?.replace("\\u002F", "/")?.replace("\\/", "/") ?: matcher.group(
                        0
                    )
                if (url.isNotEmpty() && !videoUrls.contains(url) && url.contains("mp4")) {
                    videoUrls.add(url)
                }
            }
        }

        if (videoUrls.isEmpty()) {
            val jsonPatterns = arrayOf(
                "window\\.__INITIAL_STATE__\\s*=\\s*(\\{.*?\\});?",
                "window\\.__NUXT__\\s*=\\s*(\\{.*?\\});?",
                "__DEFAULT_SCOPE__\\s*=\\s*(\\{.*?\\});?",
                "SIGI_STATE\\s*=\\s*(\\{.*?\\});?"
            )

            var foundJson = false
            jsonPatterns.forEachIndexed { index, jsonPattern ->
                if (!foundJson) {
                    val jsonMatcher = Pattern.compile(jsonPattern, Pattern.DOTALL).matcher(html)
                    if (jsonMatcher.find()) {
                        val jsonData = jsonMatcher.group(1)

                        val videoPatterns = arrayOf(
                            "https://[^\"'\\s]*\\.mp4[^\"'\\s]*",
                            "\"playAddr\"\\s*:\\s*\"([^\"]+)\"",
                            "\"downloadAddr\"\\s*:\\s*\"([^\"]+)\"",
                            "\"videoUrl\"\\s*:\\s*\"([^\"]+)\""
                        )

                        videoPatterns.forEach { videoPattern ->
                            val videoMatcher = Pattern.compile(videoPattern).matcher(jsonData ?: "")
                            while (videoMatcher.find()) {
                                val url = videoMatcher.group(1)?.replace("\\u002F", "/")
                                    ?.replace("\\/", "/") ?: videoMatcher.group(0)
                                if (url.isNotEmpty() && !videoUrls.contains(url) && url.contains("mp4")) {
                                    videoUrls.add(url)
                                }
                            }
                        }
                        foundJson = true
                    }
                }
            }
        }

        return videoUrls
    }

    private suspend fun getTwitchStreamUrls(channelName: String): Map<String, String> {
        return try {
            val twitchUrl = "https://www.twitch.tv/$channelName"

            val htmlContent = fetchWebContent(twitchUrl)

            val streamUrls = mutableMapOf<String, String>()

            val m3u8Pattern = "https://[^\\s\"']*\\.m3u8[^\\s\"']*"
            val matcher = Pattern.compile(m3u8Pattern).matcher(htmlContent)

            var count = 0
            while (matcher.find() && count < 5) {
                val streamUrl = matcher.group(0)
                val quality = when {
                    streamUrl.contains("720") -> "720p"
                    streamUrl.contains("480") -> "480p"
                    streamUrl.contains("360") -> "360p"
                    streamUrl.contains("source") -> "Source"
                    else -> "Quality_${count + 1}"
                }

                streamUrls[quality] = streamUrl
                count++
            }

            if (streamUrls.isEmpty()) {
                streamUrls["Live"] = "https://www.twitch.tv/$channelName/live"
            }

            streamUrls

        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun parseM3U8Playlist(url: String) {
        try {
            val content = fetchWebContent(url)
            val lines = content.split("\n")
            var currentTitle = ""

            withContext(Dispatchers.Main) {
                lines.forEach { line ->
                    when {
                        line.startsWith("#EXTINF:") -> {
                            currentTitle = extractM3U8Title(line)
                        }

                        line.startsWith("http") && line.trim().isNotEmpty() -> {
                            val youtubeDto = ExtractedVideo(
                                currentTitle.ifEmpty { "IPTV Channel" },
                                "",
                                line.trim(),
                                "Auto",
                                "m3u8",
                                0
                            )

                            extractedVideos.add(youtubeDto)
                            callback?.onVideoFound(youtubeDto)
                        }
                    }
                }
                callback?.onExtractionComplete(extractedVideos)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                callback?.onError("Lỗi parse M3U8: ${e.message}")
            }
        }
    }

    private fun extractM3U8Title(line: String): String {
        return try {
            val parts = line.split(",")
            if (parts.size > 1) parts[1].trim() else "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun extractVideoUrlsFromHtml(html: String, baseUrl: String): List<String> {
        val videoUrls = mutableListOf<String>()

        val patterns = arrayOf(
            "src=\"([^\"]*\\.(mp4|webm|ogg|avi|mov)[^\"]*)\"",
            "href=\"([^\"]*\\.(mp4|webm|ogg|avi|mov)[^\"]*)\"",
            "url\\(['\"]([^'\"]*\\.(mp4|webm|ogg|avi|mov)[^'\"]*)['\"]\\)",
            "https?://[^\\s\"']*\\.(mp4|webm|ogg|m3u8|avi|mov)[^\\s\"']*"
        )

        patterns.forEach { pattern ->
            val matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(html)
            while (matcher.find()) {
                val url = matcher.group(1) ?: matcher.group(0)
                if (url.isNotEmpty() && !videoUrls.contains(url)) {
                    val fullUrl = if (url.startsWith("http")) url else resolveUrl(baseUrl, url)
                    videoUrls.add(fullUrl)
                }
            }
        }

        return videoUrls
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            val base = URL(baseUrl)
            URL(base, relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }
}
package com.anrstudio.template.data.server

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.anrstudio.template.data.screenmirror.HlsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedMediaServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hlsStore: HlsStore,
) {
    private var server: NanoHTTPD? = null
    private val localTokenMap = ConcurrentHashMap<String, Uri>()
    private val remoteTokenMap = ConcurrentHashMap<String, String>()

    val port: Int get() = server?.listeningPort ?: 0

    fun start() {
        if (server?.isAlive == true) return
        val srv = object : NanoHTTPD(0) {
            override fun serve(session: IHTTPSession?): Response? {
                if (session == null) return null
                val uri = session.uri ?: return null

                Log.d(TAG, "[Unified] ${session.method} $uri")

                if (session.method == Method.OPTIONS) {
                    return corsPreflightResponse()
                }

                val isHead = session.method == Method.HEAD
                val rangeHeader = session.headers["range"]

                return when {
                    uri.startsWith("/cast/local/") -> {
                        val token = uri.removePrefix("/cast/local/").substringBefore('.')
                        val localUri = localTokenMap[token]
                        if (localUri == null) {
                            Log.w(TAG, "[Unified] local token not found: $token")
                            notFoundResponse()
                        } else {
                            serveLocalFile(localUri, rangeHeader, isHead)
                        }
                    }

                    uri.startsWith("/cast/remote/") -> {
                        val token = uri.removePrefix("/cast/remote/")
                        val remoteUrl = remoteTokenMap[token]
                        if (remoteUrl == null) {
                            Log.w(TAG, "[Unified] remote token not found: $token")
                            notFoundResponse()
                        } else {
                            Log.d(TAG, "[Unified] proxying remote: $remoteUrl")
                            serveRemoteUrl(session.headers, remoteUrl)
                        }
                    }

                    uri.startsWith("/hls/") -> {
                        serveHls(uri, isHead)
                    }

                    else -> {
                        notFoundResponse()
                    }
                }
            }
        }
        srv.start()
        server = srv
        Log.d(TAG, "[Unified] Server started on ${getLocalIpAddress()}:${srv.listeningPort}")
    }

    fun stop() {
        server?.stop()
        server = null
        localTokenMap.clear()
        remoteTokenMap.clear()
    }

    fun registerLocalUrl(uri: Uri): String? {
        if (server?.isAlive != true) {
            Log.w(TAG, "[Unified] registerLocalUrl failed: server not alive")
            return null
        }
        val ip = getLocalIpAddress()
        if (ip == null) {
            Log.w(TAG, "[Unified] registerLocalUrl failed: no local IP")
            return null
        }
        val token = UUID.randomUUID().toString().replace("-", "")
        localTokenMap[token] = uri
        val extension = fileExtension(uri)
        val url = "http://$ip:$port/cast/local/$token$extension"
        Log.d(TAG, "[Unified] registered local token=$token uri=$uri -> $url")
        return url
    }

    private fun fileExtension(uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri)
        return when {
            mimeType == "video/mp4" -> ".mp4"
            mimeType == "video/webm" -> ".webm"
            mimeType == "video/x-matroska" -> ".mkv"
            mimeType == "audio/mpeg" -> ".mp3"
            mimeType == "audio/mp4" -> ".m4a"
            mimeType == "image/jpeg" -> ".jpg"
            mimeType == "image/png" -> ".png"
            else -> ""
        }
    }

    fun registerRemoteUrl(remoteUrl: String): String? {
        if (server?.isAlive != true) {
            Log.w(TAG, "[Unified] registerRemoteUrl failed: server not alive")
            return null
        }
        val ip = getLocalIpAddress()
        if (ip == null) {
            Log.w(TAG, "[Unified] registerRemoteUrl failed: no local IP")
            return null
        }
        val token = UUID.randomUUID().toString().replace("-", "")
        remoteTokenMap[token] = remoteUrl
        val url = "http://$ip:$port/cast/remote/$token"
        Log.d(TAG, "[Unified] registered remote token=$token remoteUrl=$remoteUrl -> $url")
        return url
    }

    fun hlsPlaylistUrl(): String? {
        if (server?.isAlive != true) return null
        val ip = getLocalIpAddress() ?: return null
        return "http://$ip:$port/hls/playlist.m3u8"
    }

    private fun serveLocalFile(uri: Uri, rangeHeader: String?, isHead: Boolean): Response? {
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val fileSize = getFileSize(uri)

        if (rangeHeader != null && fileSize > 0) {
            return serveLocalRange(uri, rangeHeader, mimeType, fileSize, isHead)
        }

        Log.d(TAG, "[Unified] serving local file mime=$mimeType fileSize=$fileSize head=$isHead")
        val inputStream = if (!isHead) context.contentResolver.openInputStream(uri) else null
        val response = NanoHTTPD.newFixedLengthResponse(
            Response.Status.OK, mimeType, inputStream, if (fileSize > 0) fileSize else -1
        )
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("ETag", uri.toString() + fileSize)
        if (fileSize > 0) response.addHeader("Content-Length", fileSize.toString())
        addDlnaHeaders(response, mimeType)
        return response
    }

    private fun serveLocalRange(
        uri: Uri,
        rangeHeader: String,
        mimeType: String,
        fileSize: Long,
        isHead: Boolean,
    ): Response? {
        val match = Regex("bytes=(\\d+)-(\\d*)").find(rangeHeader)
        val start = match?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val end = match?.groupValues?.get(2)?.toLongOrNull()?.takeIf { it > 0 } ?: (fileSize - 1)
        val length = end - start + 1

        if (start >= fileSize) {
            val response = NanoHTTPD.newFixedLengthResponse(
                Response.Status.RANGE_NOT_SATISFIABLE, NanoHTTPD.MIME_PLAINTEXT, ""
            )
            response.addHeader("Content-Range", "bytes */$fileSize")
            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

        Log.d(TAG, "[Unified] serving range bytes=$start-$end mime=$mimeType head=$isHead")
        val inputStream = if (!isHead) {
            openStreamAt(uri, start)
        } else null

        val response = NanoHTTPD.newFixedLengthResponse(
            Response.Status.PARTIAL_CONTENT, mimeType, inputStream, length
        )
        response.addHeader("Content-Range", "bytes $start-$end/$fileSize")
        response.addHeader("Accept-Ranges", "bytes")
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("ETag", uri.toString() + fileSize)
        response.addHeader("Content-Length", length.toString())
        addDlnaHeaders(response, mimeType)
        return response
    }

    private fun addDlnaHeaders(response: Response, mimeType: String) {
        if (!mimeType.startsWith("video/")) return
        response.addHeader(
            "contentFeatures.dlna.org",
            "DLNA.ORG_OP=01;DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000"
        )
        response.addHeader("TransferMode.DLNA.ORG", "Streaming")
        response.addHeader("RealTimeInfo.DLNA.ORG", "DLNA.ORG_TLAG=*")
    }

    private fun openStreamAt(uri: Uri, startByte: Long): InputStream? {
        val stream = context.contentResolver.openInputStream(uri) ?: return null
        stream.skip(startByte)
        return stream
    }

    private fun serveRemoteUrl(header: Map<String, String>, remoteUrl: String): Response? {
        return try {
            val url = java.net.URL(remoteUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
            connection.setRequestProperty("Accept", "*/*")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.setRequestProperty("Accept-Encoding", "identity")
            header["range"]?.let { connection.setRequestProperty("Range", it) }
            connection.connect()

            val statusCode = connection.responseCode
            Log.d(
                TAG,
                "[Unified] remote proxy status=$statusCode contentType=${connection.contentType}"
            )

            if (statusCode == 403) {
                return forbiddenResponse("YouTube blocked request (403)")
            }

            val inputStream =
                if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val contentType = connection.contentType ?: "video/mp4"
            val contentLength = connection.contentLengthLong

            val responseStatus =
                if (statusCode == 206) Response.Status.PARTIAL_CONTENT else Response.Status.OK
            val response = NanoHTTPD.newFixedLengthResponse(
                responseStatus,
                contentType,
                inputStream,
                contentLength
            )
            connection.getHeaderField("Content-Range")
                ?.let { response.addHeader("Content-Range", it) }
            response.addHeader("Accept-Ranges", "bytes")
            response.addHeader("Access-Control-Allow-Origin", "*")
            response.addHeader("Content-Length", contentLength.toString())
            response
        } catch (e: Exception) {
            Log.e(TAG, "[Unified] remote proxy failed: ${e.message}", e)
            forbiddenResponse("Failed to proxy: ${e.message}")
        }
    }

    private fun serveHls(path: String, isHead: Boolean): Response? {
        return when {
            path == "/hls/playlist.m3u8" -> {
                val ip = getLocalIpAddress() ?: "localhost"
                val baseUrl = "http://$ip:$port"
                val data = hlsStore.playlist(baseUrl)
                val response = NanoHTTPD.newFixedLengthResponse(
                    Response.Status.OK, "application/x-mpegurl",
                    if (isHead) null else java.io.ByteArrayInputStream(data),
                    data.size.toLong(),
                )
                response.addHeader("Cache-Control", "no-cache")
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }

            path.startsWith("/hls/seg") && path.endsWith(".ts") -> {
                val segNum = path.removePrefix("/hls/seg").removeSuffix(".ts").toIntOrNull()
                val data = segNum?.let { hlsStore.getSegmentData(it) }
                if (data == null) return notFoundResponse()
                val response = NanoHTTPD.newFixedLengthResponse(
                    Response.Status.OK, "video/MP2T",
                    if (isHead) null else java.io.ByteArrayInputStream(data),
                    data.size.toLong(),
                )
                response.addHeader("Access-Control-Allow-Origin", "*")
                response
            }

            else -> notFoundResponse()
        }
    }

    private fun getFileSize(uri: Uri): Long {
        context.contentResolver.query(
            uri, arrayOf(OpenableColumns.SIZE), null, null, null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx != -1 && !cursor.isNull(idx)) return cursor.getLong(idx)
            }
        }
        return -1L
    }

    private fun corsPreflightResponse(): Response {
        return NanoHTTPD.newFixedLengthResponse(
            Response.Status.NO_CONTENT,
            NanoHTTPD.MIME_PLAINTEXT,
            ""
        )
            .apply {
                addHeader("Access-Control-Allow-Origin", "*")
                addHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS")
                addHeader("Access-Control-Allow-Headers", "Range, Content-Type")
                addHeader("Access-Control-Max-Age", "86400")
            }
    }

    private fun notFoundResponse(): Response {
        val response = NanoHTTPD.newFixedLengthResponse(
            Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "404 Not Found"
        )
        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }

    private fun forbiddenResponse(msg: String): Response {
        val response = NanoHTTPD.newFixedLengthResponse(
            Response.Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: $msg"
        )
        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }

    private fun getLocalIpAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        for (intf in interfaces.asSequence()) {
            if (!intf.isUp || intf.isLoopback) continue
            for (addr in intf.inetAddresses.asSequence()) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }

    private companion object {
        const val TAG = "cast_debug_android"
    }
}

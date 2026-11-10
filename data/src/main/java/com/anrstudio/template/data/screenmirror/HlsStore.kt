package com.hyper.castwave.data.screenmirror

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HlsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val segDir = File(context.cacheDir, "mirror_segs")
    private val _nextSegNum = AtomicInteger(0)
    private val readySegments = ArrayDeque<Int>()

    val nextSegNum: Int get() = _nextSegNum.get()

    init {
        segDir.mkdirs()
    }

    fun segmentFile(segNum: Int): File {
        segDir.mkdirs()
        return File(segDir, "seg$segNum.ts")
    }

    @Synchronized
    fun commitSegment(segNum: Int) {
        readySegments.addLast(segNum)
        _nextSegNum.set(segNum + 1)
        while (readySegments.size > MAX_SEGMENTS) {
            val old = readySegments.removeFirst()
            segmentFile(old).delete()
        }
    }

    @Synchronized
    fun playlist(baseUrl: String): ByteArray {
        val segs = readySegments.toList()
        val firstSeq = segs.firstOrNull() ?: 0
        return buildString {
            appendLine("#EXTM3U")
            appendLine("#EXT-X-VERSION:3")
            appendLine("#EXT-X-TARGETDURATION:3")
            appendLine("#EXT-X-MEDIA-SEQUENCE:$firstSeq")
            segs.forEach { n ->
                appendLine("#EXTINF:2.0,")
                appendLine("$baseUrl/hls/seg$n.ts")
            }
        }.toByteArray(Charsets.UTF_8)
    }

    fun getSegmentData(segNum: Int): ByteArray? {
        val file = segmentFile(segNum)
        return if (file.exists()) file.readBytes() else null
    }

    @Synchronized
    fun clear() {
        readySegments.clear()
        _nextSegNum.set(0)
        segDir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val MAX_SEGMENTS = 5
    }
}

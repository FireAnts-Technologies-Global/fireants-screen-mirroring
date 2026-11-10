package com.hyper.castwave.casting.mirroring

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.hyper.castwave.data.screenmirror.HlsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class ScreenMirrorEngine(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val hlsStore: HlsStore,
    private val onSegmentCommitted: (segNum: Int) -> Unit,
) {
    suspend fun run() = withContext(Dispatchers.IO) {
        val (width, height) = resolveDisplaySize()

        val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BITRATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
        }

        val codec = MediaCodec.createEncoderByType(MIME_TYPE)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = codec.createInputSurface()
        codec.start()

        var virtualDisplay: VirtualDisplay? = null
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var cachedOutputFormat: MediaFormat? = null
        var segNum = hlsStore.nextSegNum
        var segStartPts = -1L
        val bufferInfo = MediaCodec.BufferInfo()

        // Android 14+ requires registering a callback before createVirtualDisplay()
        val projectionCallback = object : MediaProjection.Callback() {}
        mediaProjection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

        try {
            virtualDisplay = mediaProjection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                width,
                height,
                context.resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                inputSurface,
                null,
                null,
            )
            codec.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) })

            while (isActive) {
                val index = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_US)

                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    cachedOutputFormat = codec.outputFormat
                    val file = hlsStore.segmentFile(segNum)
                    muxer = MediaMuxer(file.absolutePath, MUXER_FORMAT_MPEG2TS)
                    trackIndex = muxer.addTrack(cachedOutputFormat)
                    muxer.start()
                    continue
                }

                if (index < 0) continue

                val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                val isKeyFrame = bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0

                if (!isConfig && muxer != null && trackIndex >= 0) {
                    if (segStartPts < 0) segStartPts = bufferInfo.presentationTimeUs

                    val elapsedPts = bufferInfo.presentationTimeUs - segStartPts
                    val shouldRoll = isKeyFrame && elapsedPts >= SEGMENT_DURATION_US

                    if (shouldRoll && cachedOutputFormat != null) {
                        muxer.stop()
                        muxer.release()
                        hlsStore.commitSegment(segNum)
                        onSegmentCommitted(segNum)
                        segNum++
                        segStartPts = bufferInfo.presentationTimeUs

                        val newFile = hlsStore.segmentFile(segNum)
                        muxer = MediaMuxer(newFile.absolutePath, MUXER_FORMAT_MPEG2TS)
                        trackIndex = muxer.addTrack(cachedOutputFormat)
                        muxer.start()
                    }

                    val outputBuffer = codec.getOutputBuffer(index)
                    if (outputBuffer == null) {
                        codec.releaseOutputBuffer(index, false)
                        continue
                    }
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                }

                codec.releaseOutputBuffer(index, false)
            }
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { virtualDisplay?.release() }
            runCatching { codec.stop() }
            runCatching { codec.release() }
            runCatching { mediaProjection.unregisterCallback(projectionCallback) }
            runCatching { mediaProjection.stop() }
        }
    }

    private fun resolveDisplaySize(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }

        var w = metrics.widthPixels
        var h = metrics.heightPixels

        val longSide = maxOf(w, h)
        if (longSide > MAX_LONG_SIDE) {
            val scale = MAX_LONG_SIDE.toFloat() / longSide
            w = (w * scale).toInt()
            h = (h * scale).toInt()
        }

        w = w and 1.inv()
        h = h and 1.inv()

        return Pair(w, h)
    }

    private companion object {
        const val MIME_TYPE = "video/avc"
        const val MUXER_FORMAT_MPEG2TS = 2 // MUXER_FORMAT_MPEG2TS (API 26+)
        const val BITRATE = 2_000_000
        const val FRAME_RATE = 30
        const val I_FRAME_INTERVAL = 2
        const val MAX_LONG_SIDE = 1280
        const val VIRTUAL_DISPLAY_NAME = "ScreenMirror"
        const val DEQUEUE_TIMEOUT_US = 100_000L
        const val SEGMENT_DURATION_US = 2_000_000L
    }
}

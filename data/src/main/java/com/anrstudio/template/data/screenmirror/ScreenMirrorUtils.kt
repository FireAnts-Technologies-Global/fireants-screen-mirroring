package com.hyper.castwave.data.screenmirror

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ScreenMirrorUtils {
    companion object {

        @OptIn(DelicateCoroutinesApi::class)
        fun getOutputStream(
            buffer: ByteBuffer,
            displayWidth: Int,
            displayHeight: Int,
            loadImageCallback: LoadImageCallback,
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                val bitmap: Bitmap = createBitmap(displayWidth, displayHeight)

                bitmap?.copyPixelsFromBuffer(buffer)
                val stream = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                bitmap?.recycle()
                loadImageCallback.success(stream)
            }
        }
    }

    interface LoadImageCallback {
        fun success(stream: ByteArrayOutputStream?)
    }
}

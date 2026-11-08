package com.anrstudio.template.ui.component.iptv.addchannel.helper

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

object FileHelper {

    fun getFileName(context: Context, uri: Uri): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        it.getString(nameIndex) ?: "Unknown"
                    } else {
                        uri.toString()
                    }
                } else {
                    uri.toString()
                }
            } ?: uri.toString()
        } catch (e: Exception) {
            uri.path?.substringAfterLast("/", "Unknown") ?: "Unknown"
        }
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        cursor.getLong(sizeIndex)
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 MB"
        val kilobytes = bytes / 1024.0
        if (kilobytes < 1024) {
            return String.format(Locale.US, "%.1f KB", kilobytes)
        }
        val megabytes = kilobytes / 1024.0
        return String.format(Locale.US, "%.1f MB", megabytes)
    }
}

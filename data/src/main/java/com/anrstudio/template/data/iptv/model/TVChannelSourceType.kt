package com.anrstudio.template.data.iptv.model

/**
 * Enum for TV Channel Source Types
 */
enum class TVChannelSourceType(val value: String) {
    URL("URL"),           // Import from URL (M3U playlist)
    STREAM("STREAM"),     // Direct stream URL
    FILE("FILE"),         // Import from local file
    DEVICE("DEVICE");     // Import from device (USB, network share, etc.)

    companion object {
        fun fromString(value: String): TVChannelSourceType {
            return entries.find { it.value == value } ?: URL
        }
    }
}



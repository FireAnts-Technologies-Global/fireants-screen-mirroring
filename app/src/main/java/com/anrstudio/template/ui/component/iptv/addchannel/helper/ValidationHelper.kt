package com.anrstudio.template.ui.component.iptv.addchannel.helper

import android.widget.Toast
import androidx.fragment.app.Fragment
import com.anrstudio.template.R

object ValidationHelper {

    fun validatePlaylistName(fragment: Fragment, playlistName: String): Boolean {
        if (playlistName.isEmpty()) {
            Toast.makeText(
                fragment.requireContext(),
                fragment.requireContext().getString(R.string.cast_txt_please_enter_playlist_name),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    fun validateUrl(fragment: Fragment, url: String): Boolean {
        if (url.isBlank()) {
            Toast.makeText(
                fragment.requireContext(),
                fragment.requireContext().getString(R.string.cast_txt_validate_empty),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Regex chuẩn cho URL
        val urlRegex = Regex(
            "^https?://[a-zA-Z0-9.-]+(:[0-9]+)?(/.*)?$"
        )

        if (!urlRegex.matches(url)) {
            Toast.makeText(
                fragment.requireContext(),
                fragment.requireContext().getString(R.string.cast_txt_incorrect_format),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    fun validateUrlM4UM4U8(fragment: Fragment, url: String): Boolean {
        val context = fragment.requireContext()

        if (url.isBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.cast_txt_validate_empty),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Regex URL + đuôi .m3u / .m3u8
        val urlRegex = Regex(
            "^https?://[a-zA-Z0-9._%/-]+\\.(m3u8?|M3U8?)$"
        )

        if (!urlRegex.matches(url)) {
            Toast.makeText(
                context,
                context.getString(R.string.cast_txt_incorrect_format),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    fun validateFile(fragment: Fragment, fileUri: android.net.Uri?): Boolean {
        if (fileUri == null) {
            Toast.makeText(
                fragment.requireContext(),
                fragment.requireContext().getString(R.string.cast_txt_please_enter_playlist_name),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    fun validateVideos(fragment: Fragment, videos: List<*>): Boolean {
        if (videos.isEmpty()) {
            Toast.makeText(
                fragment.requireContext(),
                fragment.requireContext().getString(R.string.cast_txt_please_select_at_least_one_video),
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com", ignoreCase = true) ||
                url.contains("youtu.be", ignoreCase = true) ||
                url.contains("youtube-nocookie.com", ignoreCase = true)
    }
}



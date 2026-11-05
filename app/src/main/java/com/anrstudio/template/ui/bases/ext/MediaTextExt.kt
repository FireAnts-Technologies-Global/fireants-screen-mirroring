package com.anrstudio.template.ui.bases.ext

import android.net.Uri
import androidx.core.net.toUri
import java.text.Normalizer
import java.util.Locale
import java.util.regex.Pattern



fun String.normalText(): String {
    val nfdNormalizedString = Normalizer.normalize(this, Normalizer.Form.NFD)
    val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(nfdNormalizedString).replaceAll("").uppercase(Locale.getDefault()).trim()
}

fun String.isYouTubeLink(): Boolean {
    if (isBlank()) return false
    return try {
        val uri = toUri()
        val host = uri.host ?: return false
        host.contains("youtube.com") || host.contains("youtu.be")
    } catch (_: Exception) {
        false
    }
}

fun String.isTikTokLink(): Boolean {
    return Regex("tiktok", RegexOption.IGNORE_CASE).containsMatchIn(this)
}

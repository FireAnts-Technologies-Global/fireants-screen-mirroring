package com.anrstudio.template.navigation

import android.content.Context
import com.anrstudio.template.domain.model.MediaType

interface AppNavigator {
    fun openCastScreen(context: Context, fromMediaPicker: Boolean = false)
    fun openMediaPicker(context: Context, mediaType: MediaType)
    fun openWebBrowser(context: Context, url: String)
    fun openWebCast(context: Context)
    fun openVideoControl(context: Context)
    fun openHowToUse(context: Context)
}

package com.hyper.castwave.navigation


import android.content.Context
import android.content.Intent
import android.net.Uri
import com.hyper.castwave.domain.model.MediaType
import com.hyper.castwave.ui.component.cast.CastActivity
import com.hyper.castwave.ui.component.howtouse.HowToUseActivity
import com.hyper.castwave.ui.component.media.MediaPickerActivity
import com.hyper.castwave.ui.component.videocontrol.VideoControlActivity
import com.hyper.castwave.ui.component.webbrowser.WebBrowserActivity
import com.hyper.castwave.ui.component.webcast.WebCastActivity
import javax.inject.Inject

class AppNavigatorImpl @Inject constructor() : AppNavigator {

    override fun openCastScreen(context: Context, fromMediaPicker: Boolean) {
        context.startActivity(
            Intent(context, CastActivity::class.java).apply {
                putExtra(CastActivity.EXTRA_FROM_MEDIA_PICKER, fromMediaPicker)
            }
        )
    }

    override fun openMediaPicker(context: Context, mediaType: MediaType, pickedUri: Uri?) {
        context.startActivity(
            Intent(context, MediaPickerActivity::class.java).apply {
                putExtra(MediaPickerActivity.EXTRA_MEDIA_TYPE, mediaType.name)
                if (pickedUri != null) {
                    putExtra(MediaPickerActivity.EXTRA_PICKED_URI, pickedUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        )
    }

    override fun openWebBrowser(context: Context, url: String) {
        context.startActivity(WebBrowserActivity.newIntent(context, url))
    }

    override fun openWebCast(context: Context) {
        context.startActivity(WebCastActivity.newIntent(context))
    }

    override fun openVideoControl(context: Context) {
        context.startActivity(VideoControlActivity.newIntent(context))
    }

    override fun openHowToUse(context: Context) {
        context.startActivity(HowToUseActivity.newIntent(context))
    }
}

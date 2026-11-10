package com.hyper.castwave.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

object NetworkSettingsHelper {

    fun openWirelessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

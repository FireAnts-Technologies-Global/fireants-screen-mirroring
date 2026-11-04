package com.anrstudio.template.ui.component.uninstall.config

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

object UninstallSystemActions {

    fun openApplicationDetailsSettings(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri(),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}

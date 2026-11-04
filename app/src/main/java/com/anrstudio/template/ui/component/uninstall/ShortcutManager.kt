package com.anrstudio.template.ui.component.uninstall

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.anrstudio.template.app.AppConstants
import com.anrstudio.template.ui.bases.ext.getSystemLocaleString
import com.anrstudio.template.ui.component.uninstall.config.UninstallShortcutConfig

object ShortcutManager {

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun clearUninstallDynamicShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        try {
            val ids = UninstallShortcutConfig.dynamicShortcutItems.map { it.id }
            if (ids.isNotEmpty()) {
                manager.removeDynamicShortcuts(ids)
            }
        } catch (_: Exception) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun createDynamicShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        try {
            manager.removeAllDynamicShortcuts()
            val shortcuts = UninstallShortcutConfig.dynamicShortcutItems.map { item ->
                ShortcutInfo.Builder(context, item.id)
                    .setShortLabel(context.getSystemLocaleString(item.shortLabelRes))
                    .setIcon(Icon.createWithResource(context, item.iconRes))
                    .setIntent(
                        Intent(context, item.targetActivity).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            action = item.intentAction
                            putExtra(AppConstants.FROM_SHORTCUT, item.id)
                        },
                    )
                    .setRank(item.rank)
                    .build()
            }
            manager.dynamicShortcuts = shortcuts
        } catch (_: Exception) {
        }
    }
}

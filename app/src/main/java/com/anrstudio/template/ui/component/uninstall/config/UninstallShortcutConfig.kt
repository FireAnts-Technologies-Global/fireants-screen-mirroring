package com.anrstudio.template.ui.component.uninstall.config

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.anrstudio.template.R
import com.anrstudio.template.ui.component.uninstall.ConfirmUninstallActivity

object UninstallShortcutIds {
    const val OPEN_UNINSTALL = "action_open_uninstall"
}

object UninstallShortcutIntentActions {
    const val UNINSTALL_APP = "android.intent.action.SHORTCUT_UNINSTALL_APP"
}

data class DynamicShortcutConfig(
    val id: String,
    @StringRes val shortLabelRes: Int,
    @DrawableRes val iconRes: Int,
    val rank: Int,
    val intentAction: String = UninstallShortcutIntentActions.UNINSTALL_APP,
    val targetActivity: Class<out Activity> = ConfirmUninstallActivity::class.java,
)

object UninstallShortcutConfig {
    val dynamicShortcutItems: List<DynamicShortcutConfig> = listOf(
        DynamicShortcutConfig(
            id = UninstallShortcutIds.OPEN_UNINSTALL,
            shortLabelRes = R.string.txt_uninstall,
            iconRes = R.drawable.ic_uninstall,
            rank = 1,
        )
    )
}

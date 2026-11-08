package com.anrstudio.template.ui.component.cast

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastDialogRewardedAdBinding

/** Visual rewarded-ad prompt. The caller owns the ad SDK request and reward callback. */
class RewardedAdDialog(
    context: Context,
    private val onWatchAdRequested: () -> Unit,
    private val onDismissRequested: () -> Unit,
) {
    private val binding = CastDialogRewardedAdBinding.inflate(LayoutInflater.from(context))
    private val dialog = AlertDialog.Builder(context)
        .setView(binding.root)
        .setCancelable(true)
        .create()
        .also {
            it.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            it.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val scrimColor = ContextCompat.getColor(context, R.color.cast_cast_modal_scrim)
            it.window?.setDimAmount(Color.alpha(scrimColor) / 255f)
            it.setOnCancelListener { onDismissRequested() }
        }

    val isShowing: Boolean get() = dialog.isShowing

    init {
        binding.btnWatchAdToMirror.setOnClickListener {
            dialog.dismiss()
            onWatchAdRequested()
        }
        binding.btnRewardedMaybeLater.setOnClickListener {
            dialog.dismiss()
            onDismissRequested()
        }
    }

    fun show() {
        dialog.show()
        dialog.window?.setLayout(
            binding.root.resources.getDimensionPixelSize(R.dimen.cast_cast_dialog_width),
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    fun dismiss() = dialog.dismiss()
}

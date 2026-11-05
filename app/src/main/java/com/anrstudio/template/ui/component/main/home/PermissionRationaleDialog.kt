package com.anrstudio.template.ui.component.main.home

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import com.anrstudio.template.databinding.CastDialogPermissionBinding

class PermissionRationaleDialog(
    context: Context,
    @DrawableRes iconRes: Int,
    title: String,
    message: String,
    allowText: String,
    denyText: String,
    onAllow: () -> Unit,
    onDeny: () -> Unit,
) {
    private val binding = CastDialogPermissionBinding.inflate(LayoutInflater.from(context))

    private val dialog = AlertDialog.Builder(context)
        .setView(binding.root)
        .setCancelable(false)
        .create()
        .also { it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) }

    val isShowing: Boolean get() = dialog.isShowing

    init {
        binding.imgPermission.setImageResource(iconRes)
        binding.tvPermissionTitle.text = title
        binding.tvPermissionMessage.text = message
        binding.btnPermissionAllow.text = allowText
        binding.btnPermissionDeny.text = denyText

        binding.btnPermissionAllow.setOnClickListener {
            dialog.dismiss()
            onAllow()
        }
        binding.btnPermissionDeny.setOnClickListener {
            dialog.dismiss()
            onDeny()
        }
    }

    fun show() = dialog.show()
    fun dismiss() = dialog.dismiss()
}

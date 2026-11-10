package com.hyper.castwave.ui.component.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import com.hyper.castwave.databinding.CastDialogLoadingBinding
import com.hyper.castwave.databinding.CastDialogPinBinding

class LoadingDialog(
    context: Context,
) {
    private val binding = CastDialogLoadingBinding.inflate(LayoutInflater.from(context))
    val isShowing: Boolean get() = dialog.isShowing

    private val dialog = AlertDialog.Builder(context)
        .setView(binding.root)
        .setCancelable(false)
        .create()
        .also { it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) }

    fun show() {
        dialog.show()
    }

    fun dismiss() = dialog.dismiss()

}

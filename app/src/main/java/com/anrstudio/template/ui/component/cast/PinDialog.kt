package com.anrstudio.template.ui.component.cast

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastDialogPinBinding

class PinDialog(
    context: Context,
    deviceName: String,
    private val onConfirm: (pin: String) -> Unit,
    private val onCancel: () -> Unit,
) {
    private val binding = CastDialogPinBinding.inflate(LayoutInflater.from(context))

    private val dialog = AlertDialog.Builder(context)
        .setView(binding.root)
        .setCancelable(false)
        .create()
        .also { it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) }

    val isShowing: Boolean get() = dialog.isShowing

    init {
        binding.tvPinSubtitle.text = context.getString(R.string.cast_pin_dialog_subtitle, deviceName)
        setupButtons()
    }

    fun show() {
        dialog.show()
        binding.etPin.postDelayed({
            binding.etPin.requestFocus()
            val imm = binding.etPin.context
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etPin, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    fun dismiss() = dialog.dismiss()

    fun showError(message: String) {
        binding.tvPinError.text = message
        binding.tvPinError.visibility = View.VISIBLE
        binding.etPin.text?.clear()
        binding.etPin.requestFocus()
    }

    private fun setupButtons() {
        binding.btnPinCancel.setOnClickListener {
            dismiss()
            onCancel()
        }

        binding.btnPinConfirm.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()
            if (pin.length < 4) {
                binding.tvPinError.text =
                    binding.root.context.getString(R.string.cast_pin_error_incomplete)
                binding.tvPinError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            dismiss()
            onConfirm(pin)
        }
    }
}

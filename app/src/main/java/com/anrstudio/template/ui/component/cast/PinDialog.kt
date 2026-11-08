package com.anrstudio.template.ui.component.cast

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
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
        .also {
            it.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            it.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val scrimColor = ContextCompat.getColor(context, R.color.cast_cast_modal_scrim)
            it.window?.setDimAmount(Color.alpha(scrimColor) / 255f)
        }

    val isShowing: Boolean get() = dialog.isShowing

    init {
        binding.tvPinSubtitle.text = context.getString(R.string.cast_pin_dialog_subtitle, deviceName)
        binding.etPin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderInputState(!s.isNullOrEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        renderInputState(entered = false)
        setupButtons()
    }

    fun show() {
        dialog.show()
        dialog.window?.setLayout(
            binding.root.resources.getDimensionPixelSize(R.dimen.cast_cast_dialog_width),
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
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

    private fun renderInputState(entered: Boolean) {
        binding.etPin.setBackgroundResource(
            if (entered) R.drawable.cast_bg_pin_dialog_input_entered
            else R.drawable.cast_bg_pin_dialog_input_empty
        )
        binding.etPin.setTextColor(
            ContextCompat.getColor(
                binding.root.context,
                if (entered) R.color.cast_cast_primary_text else R.color.cast_cast_input_text,
            )
        )
        binding.etPin.typeface = androidx.core.content.res.ResourcesCompat.getFont(
            binding.root.context,
            if (entered) R.font.inter_semibold else R.font.inter_regular,
        )
        binding.etPin.layoutParams = binding.etPin.layoutParams.apply {
            height = binding.root.resources.getDimensionPixelSize(
                if (entered) R.dimen.cast_cast_dialog_input_entered_height
                else R.dimen.cast_cast_dialog_input_empty_height,
            )
        }
        binding.btnPinConfirm.setBackgroundResource(
            if (entered) R.drawable.cast_bg_pin_dialog_button_filled
            else R.drawable.cast_bg_pin_dialog_button_outline
        )
        binding.btnPinConfirm.setTextColor(
            ContextCompat.getColor(
                binding.root.context,
                if (entered) android.R.color.white else R.color.cast_cast_input_text,
            )
        )
    }
}

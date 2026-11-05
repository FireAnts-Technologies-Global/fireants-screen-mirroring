package com.anrstudio.template.ui.component.iptv.dialog

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.content.ContextCompat
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastDialogEditChannelBinding
import com.anrstudio.template.databinding.CastDialogEditPlaylistBinding
import com.anrstudio.template.ui.bases.BaseDialog
import com.anrstudio.template.ui.bases.ext.click

class EditChannelDialog(
    context: Context,
    private val currentName: String = "",
    private val onSaveClick: (String) -> Unit
) : BaseDialog<CastDialogEditChannelBinding>(context, R.style.cast_BaseDialog) {

    override fun getLayoutDialog(): Int {
        return R.layout.cast_dialog_edit_channel
    }

    override fun initViews() {
        super.initViews()
        mBinding.etChannelName.setText(currentName)
        if (currentName.isNotEmpty()) {
            mBinding.icClearInput.visibility = View.VISIBLE
        }
    }

    override fun onClickViews() {
        super.onClickViews()
        mBinding.etChannelName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()

                // Enable / disable button
                mBinding.btnSave.isEnabled = hasText
                mBinding.btnSave.isClickable = hasText

                // Đổi màu theo trạng thái
                val color =
                    if (hasText) R.color.cast_color_button_enable else R.color.cast_color_button_disable
                mBinding.btnSave.shapeDrawableBuilder
                    .setSolidColor(ContextCompat.getColor(context, color))
                    .intoBackground()

                // Alpha để nhìn rõ disabled
                mBinding.btnSave.alpha = if (hasText) 1f else 0.4f

                // Clear icon
                mBinding.icClearInput.visibility = if (hasText) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        mBinding.icClose.click {
            dismiss()
        }

        mBinding.icClearInput.click {
            mBinding.etChannelName.setText("")
            mBinding.icClearInput.visibility = View.GONE
        }

        mBinding.btnSave.click {
            val playlistName = mBinding.etChannelName.text?.toString()?.trim() ?: ""
            if (playlistName.isNotEmpty()) {
                onSaveClick(playlistName)
                dismiss()
            }
        }

        mBinding.etChannelName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mBinding.icClearInput.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
}


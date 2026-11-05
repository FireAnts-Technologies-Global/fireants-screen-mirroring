package com.anrstudio.template.ui.component.iptv.dialog

import android.content.Context
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastDialogDeleteChannelBinding
import com.anrstudio.template.databinding.CastDialogDeletePlaylistBinding
import com.anrstudio.template.ui.bases.BaseDialog
import com.anrstudio.template.ui.bases.ext.click

class DeleteChannelDialog(
    context: Context,
    private val onDeleteClick: () -> Unit
) : BaseDialog<CastDialogDeleteChannelBinding>(context, R.style.cast_BaseDialog) {

    override fun getLayoutDialog(): Int {
        return R.layout.cast_dialog_delete_channel
    }

    override fun onResizeViews() {
        super.onResizeViews()
    }

    override fun onClickViews() {
        super.onClickViews()
        mBinding.btnCancel.click {
            dismiss()
        }

        mBinding.btnDelete.click {
            onDeleteClick()
            dismiss()
        }
    }
}


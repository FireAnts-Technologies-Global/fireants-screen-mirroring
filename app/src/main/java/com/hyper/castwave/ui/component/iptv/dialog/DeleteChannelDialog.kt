package com.hyper.castwave.ui.component.iptv.dialog

import android.content.Context
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastDialogDeleteChannelBinding
import com.hyper.castwave.databinding.CastDialogDeletePlaylistBinding
import com.hyper.castwave.ui.bases.BaseDialog
import com.hyper.castwave.ui.bases.ext.click

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


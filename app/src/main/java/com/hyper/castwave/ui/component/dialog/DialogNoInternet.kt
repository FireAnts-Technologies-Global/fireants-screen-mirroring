package com.hyper.castwave.ui.component.dialog

import android.app.Dialog
import android.content.Context
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastDialogNoInternetBinding
import com.hyper.castwave.ui.bases.BaseDialog
import com.hyper.castwave.ui.bases.ext.click
import com.hyper.castwave.utils.NetworkSettingsHelper

class DialogNoInternet(context: Context) : BaseDialog<CastDialogNoInternetBinding>(context, R.style.cast_ThemeDialog) {

    override fun getLayoutDialog(): Int = R.layout.cast_dialog_no_internet

    override fun initViews() {
        super.initViews()
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        mBinding.btnOpenSetting.click {
            NetworkSettingsHelper.openWirelessSettings(context)
        }
    }
}

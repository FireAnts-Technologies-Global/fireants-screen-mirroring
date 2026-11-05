package com.anrstudio.template.ui.component.dialog

import android.app.Dialog
import android.content.Context
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastDialogNoInternetBinding
import com.anrstudio.template.ui.bases.BaseDialog
import com.anrstudio.template.ui.bases.ext.click
import com.anrstudio.template.utils.NetworkSettingsHelper

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

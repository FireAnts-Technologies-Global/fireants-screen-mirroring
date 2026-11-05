package com.anrstudio.template.ui.component.iptv.dialog

import android.content.Context
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastDialogNoInternetBinding
import com.anrstudio.template.ui.bases.BaseDialogFragment
import com.anrstudio.template.ui.bases.ext.click
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NoInternetDialog : BaseDialogFragment<CastDialogNoInternetBinding>() {

    override fun getLayoutDialog() = R.layout.cast_dialog_no_internet

    companion object {
        const val TAG_NO_INTERNET_DIALOG = "tag_no_internet_dialog"

        fun newInstance() = NoInternetDialog()
    }

    private var listener: NoInternetDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = try {
            parentFragment as NoInternetDialogListener
        } catch (_: Exception) {
            try {
                context as NoInternetDialogListener
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun initViews() {
        super.initViews()
        isCancelable = false
    }

    override fun onClickViews() {
        super.onClickViews()
        mBinding.btnOpenSetting.click {
            listener?.onOpenSetting()
        }
    }

    interface NoInternetDialogListener {
        fun onOpenSetting()
    }
}

package com.anrstudio.template.ui.component.iptv.dialog

import android.content.Context
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastDialogVideoErrorBinding
import com.anrstudio.template.ui.bases.BaseDialogFragment
import com.anrstudio.template.ui.bases.ext.click
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoErrorDialog : BaseDialogFragment<CastDialogVideoErrorBinding>() {

    override fun getLayoutDialog() = R.layout.cast_dialog_video_error

    companion object {

        const val TAG_VIDEO_ERROR_DIALOG = "tag_video_error_dialog"

        fun newInstance() = VideoErrorDialog()
    }

    private var listener: VideoErrorDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as VideoErrorDialogListener
        } catch (_: Exception) {
            try {
                listener = context as VideoErrorDialogListener
            } catch (_: Exception) {
            }
        }
    }

    override fun initViews() {
        super.initViews()
        isCancelable = false
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.btnCancel.click {
            dismiss()
        }

        mBinding.btnNext.click {
            listener?.onNextChanelWhenError()
            dismiss()
        }
    }

    interface VideoErrorDialogListener {

        fun onNextChanelWhenError()
    }
}

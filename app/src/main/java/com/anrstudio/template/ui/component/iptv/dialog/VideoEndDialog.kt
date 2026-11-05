package com.anrstudio.template.ui.component.iptv.dialog

import android.content.Context
import androidx.core.os.bundleOf
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastDialogErrorBinding
import com.anrstudio.template.databinding.CastDialogVideoEndBinding
import com.anrstudio.template.databinding.CastDialogVideoErrorBinding
import com.anrstudio.template.ui.bases.BaseDialogFragment
import com.anrstudio.template.ui.bases.ext.click
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoEndDialog : BaseDialogFragment<CastDialogVideoEndBinding>() {

    override fun getLayoutDialog() = R.layout.cast_dialog_video_end

    companion object {

        const val TAG_VIDEO_END_DIALOG = "tag_video_end_dialog"

        fun newInstance() = VideoEndDialog()
    }

    private var listener: VideoEndDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as VideoEndDialogListener
        } catch (_: Exception) {
            try {
                listener = context as VideoEndDialogListener
            } catch (_: Exception) {
            }
        }
    }

    override fun initViews() {
        super.initViews()
        isCancelable = true
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.btnCancel.click {
            dismiss()
        }

        mBinding.btnNext.click {
            listener?.onNextChanelWhenEnd()
            dismiss()
        }
    }

    interface VideoEndDialogListener {

        fun onNextChanelWhenEnd()
    }
}

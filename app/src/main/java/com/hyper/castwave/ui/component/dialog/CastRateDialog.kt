package com.hyper.castwave.ui.component.dialog

import android.content.Context
import android.widget.Toast
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastDialogRateAppBinding
import com.hyper.castwave.ui.bases.BaseDialog
import com.hyper.castwave.ui.bases.ext.click
import com.hyper.castwave.ui.component.custom.CustomRatingBar


class CastRateDialog(
    val mContext: Context,
    val onRating: () -> Unit,
    val onNormalRate: () -> Unit,
) : BaseDialog<CastDialogRateAppBinding>(
    mContext,
    R.style.cast_BaseDialog
) {


    var previousRate = 0f
    override fun getLayoutDialog(): Int {
        return R.layout.cast_dialog_rate_app
    }

    override fun onResizeViews() {
        super.onResizeViews()
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.outView.click {
            dismiss()
        }

        mBinding.icClose.click {
            dismiss()
        }

        mBinding.simpleRatingBar.setOnRatingChangeListener(object :
            CustomRatingBar.OnRatingChangeListener {
            override fun onRatingChanged(ratingBar: CustomRatingBar, rating: Int) {
                val currentRating = rating
                if (currentRating <= 4) {
                    onNormalRate.invoke()
                    dismiss()
                } else {
                    onRating.invoke()
                    dismiss()
                }
                when (rating) {
                    0 -> {}

                    1 -> {}

                    2 -> {}

                    3 -> {}

                    4 -> {}

                    5 -> {}
                }
                previousRate = rating.toFloat()
            }
        })
    }
}

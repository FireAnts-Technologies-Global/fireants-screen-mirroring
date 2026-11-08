package com.anrstudio.template.ui.component.howtouse

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.WindowManager
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastActivityHowToUseBinding
import com.anrstudio.template.databinding.CastItemHowToUseStepBinding
import com.anrstudio.template.ui.bases.BaseActivity
import com.anrstudio.template.ui.bases.StatusBarConfig
import com.anrstudio.template.ui.bases.ext.click

class HowToUseActivity : BaseActivity<CastActivityHowToUseBinding>() {

    override val statusBarConfig = StatusBarConfig(
        isVisible = true,
        applyPadding = false,
        barColor = Color.TRANSPARENT,
        lightIcons = true,
    )

    override fun requestWindow() {
        super.requestWindow()
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    }

    override fun getLayoutActivity(): Int = R.layout.cast_activity_how_to_use

    override fun initViews() {
        super.initViews()
        bindStep(
            binding = mBinding.step1,
            stepNumber = "01",
            iconRes = R.drawable.cast_how_to_use_icon_wifi,
            titleRes = R.string.cast_how_to_use_guide_step_1_title,
            descRes = R.string.cast_how_to_use_guide_step_1_desc,
        )
        bindStep(
            binding = mBinding.step2,
            stepNumber = "02",
            iconRes = R.drawable.cast_how_to_use_icon_grid,
            titleRes = R.string.cast_how_to_use_guide_step_2_title,
            descRes = R.string.cast_how_to_use_guide_step_2_desc,
        )
        bindStep(
            binding = mBinding.step3,
            stepNumber = "03",
            iconRes = R.drawable.cast_how_to_use_icon_play,
            titleRes = R.string.cast_how_to_use_guide_step_3_title,
            descRes = R.string.cast_how_to_use_guide_step_3_desc,
        )
    }

    override fun onClickViews() {
        super.onClickViews()
        mBinding.btnBack.click { finish() }
    }

    private fun bindStep(
        binding: CastItemHowToUseStepBinding,
        stepNumber: String,
        iconRes: Int,
        titleRes: Int,
        descRes: Int,
    ) {
        binding.tvStepNumber.text = stepNumber
        binding.ivStepIcon.setImageResource(iconRes)
        binding.tvStepTitle.setText(titleRes)
        binding.tvStepDesc.setText(descRes)
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, HowToUseActivity::class.java)
    }
}

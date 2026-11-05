package com.anrstudio.template.ui.component.howtouse

import android.content.Context
import android.content.Intent
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastActivityHowToUseBinding
import com.anrstudio.template.databinding.CastItemHowToUseStepBinding
import com.anrstudio.template.ui.bases.BaseActivity
import com.anrstudio.template.ui.bases.ext.click

class HowToUseActivity : BaseActivity<CastActivityHowToUseBinding>() {

    override fun getLayoutActivity(): Int = R.layout.cast_activity_how_to_use

    override fun initViews() {
        super.initViews()
        bindStep(
            binding = mBinding.step1,
            stepNumber = getString(R.string.cast_one),
            titleRes = R.string.cast_how_to_use_step_1_title,
            descRes = R.string.cast_how_to_use_step_1_desc,
        )
        bindStep(
            binding = mBinding.step2,
            stepNumber = getString(R.string.cast_two),
            titleRes = R.string.cast_how_to_use_step_2_title,
            descRes = R.string.cast_how_to_use_step_2_desc,
        )
        bindStep(
            binding = mBinding.step3,
            stepNumber = getString(R.string.cast_three),
            titleRes = R.string.cast_how_to_use_step_3_title,
            descRes = R.string.cast_how_to_use_step_3_desc,
        )
    }

    override fun onClickViews() {
        super.onClickViews()
        mBinding.btnBack.click { finish() }
    }

    private fun bindStep(
        binding: CastItemHowToUseStepBinding,
        stepNumber: String,
        titleRes: Int,
        descRes: Int,
    ) {
        binding.tvStepNumber.text = stepNumber
        binding.tvStepTitle.setText(titleRes)
        binding.tvStepDesc.setText(descRes)
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, HowToUseActivity::class.java)
    }
}

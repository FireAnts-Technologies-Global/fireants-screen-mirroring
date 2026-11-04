package com.anrstudio.template.ui.component.uninstall

import android.widget.CheckBox
import android.widget.TextView
import com.anrstudio.ads.ads.wrapper.ApNativeAd
import com.anrstudio.template.R
import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.populateNativeAdView
import com.anrstudio.template.databinding.ActivitySurveyBinding
import com.anrstudio.template.ui.bases.BaseActivity
import com.anrstudio.template.ui.bases.ext.click
import com.anrstudio.template.ui.bases.ext.goneView
import com.anrstudio.template.ui.bases.ext.visibleView
import com.anrstudio.template.ui.component.uninstall.config.UninstallSurveyConfig
import com.anrstudio.template.ui.component.uninstall.config.UninstallSystemActions
import com.anrstudio.template.utils.Routes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SurveyActivity : BaseActivity<ActivitySurveyBinding>() {

    override fun getLayoutActivity() = R.layout.activity_survey

    override fun initViews() {
        super.initViews()
        bindSurveyFromSettings()
        AdsManager.loadNativeSurvey(this, R.layout.layout_native_survey)
    }

    override fun observeData() {
        super.observeData()
        AdsManager.nativeSurveyAdLive.observe(this) { ad -> renderSurveyAd(ad) }
    }

    private fun renderSurveyAd(ad: ApNativeAd?) {
        if (ad == null) {
            mBinding.frAds.goneView()
            return
        }
        mBinding.frAds.visibleView()
        populateNativeAdView(
            this,
            ad,
            mBinding.frAds,
            mBinding.shimmerAds.shimmerNativeMedium
        )
    }

    private fun bindSurveyFromSettings() {
        mBinding.tvSurveySectionTitle.setText(UninstallSurveyConfig.sectionTitleRes)

        val container = mBinding.surveyReasonsContainer
        container.removeAllViews()

        val inflater = layoutInflater
        UninstallSurveyConfig.reasonOptions.forEach { option ->
            val row = inflater.inflate(R.layout.item_survey_reason, container, false)
            row.findViewById<TextView>(R.id.tv_label).setText(option.labelRes)
            row.findViewById<CheckBox>(R.id.checkbox_reason).apply {
                isChecked = option.defaultChecked
                tag = option.id
            }
            container.addView(row)
        }
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.btnCancel.click {
            whenBack()
        }

        mBinding.icBack.click {
            whenBack()
        }

        mBinding.btnUninstall.click {
            UninstallSystemActions.openApplicationDetailsSettings(this)
            finish()
        }
    }

    private fun whenBack() {
        Routes.startMainActivity(this)
        finish()
    }
}

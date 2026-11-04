package com.anrstudio.template.ui.component.uninstall

import com.anrstudio.ads.ads.wrapper.ApNativeAd
import com.anrstudio.template.R
import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.populateNativeAdView
import com.anrstudio.template.databinding.ActivityConfirmUninstallBinding
import com.anrstudio.template.ui.bases.BaseActivity
import com.anrstudio.template.ui.bases.ext.click
import com.anrstudio.template.ui.bases.ext.goneView
import com.anrstudio.template.ui.bases.ext.visibleView
import com.anrstudio.template.utils.Routes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmUninstallActivity : BaseActivity<ActivityConfirmUninstallBinding>() {

    override fun getLayoutActivity() = R.layout.activity_confirm_uninstall

    override fun initViews() {
        super.initViews()
        AdsManager.loadNativeConfirmUninstall(this, R.layout.layout_native_uninstall)
    }

    override fun observeData() {
        super.observeData()
        AdsManager.nativeConfirmUninstallAdLive.observe(this) { ad -> renderConfirmAd(ad) }
    }

    private fun renderConfirmAd(ad: ApNativeAd?) {
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

    override fun onClickViews() {
        super.onClickViews()

        mBinding.icBack.click {
            whenBack()
        }

        mBinding.tryAgain.click {
            whenBack()
        }

        mBinding.tvStillWant.click {
            Routes.startSurveyActivity(this)
            finish()
        }

    }


    private fun whenBack() {
        Routes.startMainActivity(this)
        finish()
    }

    override fun onBackPressed() {
        Routes.startMainActivity(this)
    }
}
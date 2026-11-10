package com.hyper.castwave.ui.component.welcome

import com.anrstudio.ads.ads.wrapper.ApNativeAd
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.ads.populateNativeAdView
import com.hyper.castwave.databinding.ActivityWelcomeBinding
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.ui.bases.ext.click
import com.hyper.castwave.ui.bases.ext.goneView
import com.hyper.castwave.ui.bases.ext.isNetwork
import com.hyper.castwave.ui.bases.ext.visibleView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>() {
    override fun getLayoutActivity(): Int {
        return R.layout.activity_welcome
    }

    override fun initViews() {
        super.initViews()
        showWelcomeAdLoading()
        AdsManager.loadNativeWelcome(this, R.layout.layout_native_welcome)
    }

    override fun observeData() {
        super.observeData()
        AdsManager.nativeWelcomeAdStateLive.observe(this) { state ->
            when (state) {
                AdsManager.NativeAdLoadState.IDLE,
                AdsManager.NativeAdLoadState.LOADING -> showWelcomeAdLoading()

                AdsManager.NativeAdLoadState.LOADED -> Unit
                AdsManager.NativeAdLoadState.FAILED -> hideWelcomeAd()
            }
        }
        AdsManager.nativeWelcomeAdLive.observe(this) { ad ->
            if (ad != null) renderWelcomeAd(ad)
        }
    }

    override fun onClickViews() {
        super.onClickViews()
        mBinding.btnStart.click {
            AdsManager.loadAndShowInterWelcome(this) {
                finish()
            }
        }
    }

    private fun showWelcomeAdLoading() {
        mBinding.frAds.visibleView()
        mBinding.frAdContent.goneView()
        mBinding.shimmerAds.shimmerNativeLarge.visibleView()
        mBinding.shimmerAds.shimmerNativeLarge.startShimmer()
    }

    private fun hideWelcomeAd() {
        mBinding.shimmerAds.shimmerNativeLarge.stopShimmer()
        mBinding.frAdContent.goneView()
        mBinding.frAds.goneView()
    }

    private fun renderWelcomeAd(ad: ApNativeAd) {
        if (!isNetwork()) {
            hideWelcomeAd()
            return
        }
        mBinding.frAds.visibleView()
        mBinding.frAdContent.visibleView()
        populateNativeAdView(
            this,
            ad,
            mBinding.frAdContent,
            mBinding.shimmerAds.shimmerNativeLarge
        )
    }
}

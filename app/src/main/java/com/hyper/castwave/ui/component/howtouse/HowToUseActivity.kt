package com.hyper.castwave.ui.component.howtouse

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.WindowManager
import com.anrstudio.ads.ads.wrapper.ApNativeAd
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.ads.populateNativeAdView
import com.hyper.castwave.databinding.CastActivityHowToUseBinding
import com.hyper.castwave.databinding.CastItemHowToUseStepBinding
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.ui.bases.StatusBarConfig
import com.hyper.castwave.ui.bases.ext.click
import com.hyper.castwave.ui.bases.ext.goneView
import com.hyper.castwave.ui.bases.ext.isNetwork
import com.hyper.castwave.ui.bases.ext.visibleView

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
        showAdLoading()
        AdsManager.loadNativeHowToUse(this, R.layout.layout_native_ad_medium)
    }

    override fun observeData() {
        super.observeData()
        AdsManager.nativeHowToUseAdStateLive.observe(this) { state ->
            when (state) {
                AdsManager.NativeAdLoadState.IDLE,
                AdsManager.NativeAdLoadState.LOADING -> showAdLoading()

                AdsManager.NativeAdLoadState.LOADED -> Unit
                AdsManager.NativeAdLoadState.FAILED -> hideAd()
            }
        }
        AdsManager.nativeHowToUseAdLive.observe(this) { ad ->
            if (ad != null) renderAd(ad)
        }
    }

    override fun onClickViews() {
        super.onClickViews()
        mBinding.btnBack.click { finish() }
    }

    private fun showAdLoading() {
        mBinding.flAds.visibleView()
        mBinding.flAdContent.goneView()
        mBinding.shimmerAds.shimmerNativeMedium.visibleView()
        mBinding.shimmerAds.shimmerNativeMedium.startShimmer()
    }

    private fun hideAd() {
        mBinding.shimmerAds.shimmerNativeMedium.stopShimmer()
        mBinding.flAdContent.goneView()
        mBinding.flAds.goneView()
    }

    private fun renderAd(ad: ApNativeAd) {
        if (!isNetwork()) {
            hideAd()
            return
        }
        mBinding.flAds.visibleView()
        mBinding.flAdContent.visibleView()
        populateNativeAdView(
            this,
            ad,
            mBinding.flAdContent,
            mBinding.shimmerAds.shimmerNativeMedium,
        )
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

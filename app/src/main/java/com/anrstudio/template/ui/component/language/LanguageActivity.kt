package com.anrstudio.template.ui.component.language

import com.anrstudio.ads.ads.wrapper.ApNativeAd
import com.anrstudio.template.R
import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.AdsManager.loadNativeLanguageClick
import com.anrstudio.template.ads.RemoteConfigUtils
import com.anrstudio.template.ads.populateNativeAdView
import com.anrstudio.template.app.AppConstants
import com.anrstudio.template.app.AppConstants.DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON
import com.anrstudio.template.databinding.ActivityLanguageBinding
import com.anrstudio.template.ui.bases.BaseActivity
import com.anrstudio.template.ui.bases.NavigationBarConfig
import com.anrstudio.template.ui.bases.ext.click
import com.anrstudio.template.ui.bases.ext.goneView
import com.anrstudio.template.ui.bases.ext.isNetwork
import com.anrstudio.template.ui.bases.ext.visibleView
import com.anrstudio.template.ui.component.language.adapter.LanguageAdapter
import com.anrstudio.template.ui.component.language.data.LanguageData
import com.anrstudio.template.utils.Routes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    private var timeDelayDoneButton = DEFAULT_TIME_DELAY_SHOW_LANGUAGE_DONE_BUTTON

    override val navigationBarConfig: NavigationBarConfig
        get() = NavigationBarConfig(
            isVisible = RemoteConfigUtils.getOnShowNavigationButton(),
            applyPadding = RemoteConfigUtils.getOnShowNavigationButton()
        )

    override fun getLayoutActivity() = R.layout.activity_language

    private val fromSetting
        get() = intent.getBooleanExtra(AppConstants.KEY_SETTING, false)
    private var shouldDelayDoneButton = true
    private var selectedIso = LanguageData.defaultLanguage.iso

    private val languageAdapter: LanguageAdapter by lazy {
        LanguageAdapter(
            context = this@LanguageActivity,
            onItemLanguageClick = {
                if (!fromSetting) {
                    listenLanguageClickAd()
                }
                enableDoneButton()
                selectedIso = it.iso
                LanguageData.selectLanguage(it.iso)
                resubmitLanguageData()
            }
        )
    }

    override fun initViews() {
        LanguageData.clearSelection()
        forceEnableShowAd()
        shouldDelayDoneButton = RemoteConfigUtils.shouldDelayLanguageDoneButton()
        timeDelayDoneButton = RemoteConfigUtils.getTimeDelayButtonDoneLanguage()
        initAdapter()
        initLayout()

        mBinding.root.postDelayed({
            if (!fromSetting) {
                loadNativeLanguageClick(
                    this,
                    appSharedPref.firstLanguage,
                    R.layout.layout_native_language_click
                )
            }
            initAds()
        }, 100L)
    }

    override fun observeData() {
        super.observeData()
        if (!fromSetting) {
            listenLanguageAd()
        } else {
            hideLanguageAd()
        }
    }

    override fun onClickViews() {
        mBinding.imgBack.click {
            onBackPressed()
        }
        mBinding.tvDone.click {
            val iso = selectedIso
            appSharedPref.languageCode = iso
            startNextActivity()
        }

    }

    private fun initAdapter() {
        mBinding.recyclerView.adapter = languageAdapter
        resubmitLanguageData()
    }

    private fun initLayout() {
        mBinding.tvDone.visibleView()

        if (fromSetting) {
            mBinding.tvDone.isEnabled = true
            timeDelayDoneButton = 0
            mBinding.imgBack.visibleView()
        } else {
            mBinding.tvDone.isEnabled = false
            mBinding.imgBack.goneView()
        }
    }

    private fun initAds() {
        if (fromSetting) {
            hideLanguageAd()
        } else {
            AdsManager.loadNativeOnboarding4(
                this,
                appSharedPref.firstOnBoarding,
                R.layout.layout_native_onboarding
            )
        }
    }

    private fun listenLanguageAd() {
        AdsManager.nativeLanguageClickAdLive.removeObservers(this)
        AdsManager.nativeLanguageClickAdStateLive.removeObservers(this)
        AdsManager.nativeLanguageAdStateLive.observe(this) { state ->
            renderLanguageAdState(state)
        }
        AdsManager.nativeLanguageAdLive.observe(this) { ad ->
            if (ad != null) showNativeLanguage(ad)
        }
    }

    private fun listenLanguageClickAd() {
        AdsManager.nativeLanguageAdLive.removeObservers(this)
        AdsManager.nativeLanguageAdStateLive.removeObservers(this)
        AdsManager.nativeLanguageClickAdStateLive.removeObservers(this)
        showLanguageAdLoading()
        AdsManager.nativeLanguageClickAdStateLive.observe(this) { state ->
            renderLanguageAdState(state)
        }
        AdsManager.nativeLanguageClickAdLive.observe(this) { ad ->
            if (ad != null) showNativeLanguage(ad)
        }
    }

    private fun renderLanguageAdState(state: AdsManager.NativeAdLoadState) {
        when (state) {
            AdsManager.NativeAdLoadState.IDLE,
            AdsManager.NativeAdLoadState.LOADING -> showLanguageAdLoading()

            AdsManager.NativeAdLoadState.LOADED -> Unit
            AdsManager.NativeAdLoadState.FAILED -> hideLanguageAd()
        }
    }

    private fun showLanguageAdLoading() {
        if (fromSetting) return
        mBinding.flAds.visibleView()
        mBinding.flAdContent.goneView()
        mBinding.shimmerAds.shimmerNativeLarge.visibleView()
        mBinding.shimmerAds.shimmerNativeLarge.startShimmer()
    }

    private fun hideLanguageAd() {
        mBinding.shimmerAds.shimmerNativeLarge.stopShimmer()
        mBinding.flAdContent.goneView()
        mBinding.flAds.goneView()
    }

    private fun showNativeLanguage(ad: ApNativeAd) {
        if (fromSetting) {
            hideLanguageAd()
            return
        }
        if (!isNetwork()) {
            hideLanguageAd()
            return
        }
        mBinding.flAds.visibleView()
        mBinding.flAdContent.visibleView()
        populateNativeAdView(
            this,
            ad,
            mBinding.flAdContent,
            mBinding.shimmerAds.shimmerNativeLarge
        )
    }

    private fun enableDoneButton() {
        if (mBinding.tvDone.isEnabled) return

        if (shouldDelayDoneButton && timeDelayDoneButton > 0) {
            mBinding.tvDone.postDelayed({
                mBinding.tvDone.isEnabled = true
            }, timeDelayDoneButton)
        } else {
            mBinding.tvDone.isEnabled = true
        }
    }

    private fun resubmitLanguageData() {
        languageAdapter.submitList(LanguageData.languages)
    }

    private fun startNextActivity() {
        if (fromSetting) {
            Routes.startMainActivity(this)
        } else {
            appSharedPref.firstLanguage = false
            Routes.startOnBoardingActivity(this)
        }
        finish()
    }

    private fun forceEnableShowAd() {
//        mBinding.tvTitle.setOnAdminAdToggleListener {
//            Routes.startSplashActivity(this@LanguageActivity)
//            finish()
//        }
    }
}

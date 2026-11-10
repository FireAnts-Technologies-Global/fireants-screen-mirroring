package com.hyper.castwave.ui.component.splash

import android.annotation.SuppressLint
import android.os.CountDownTimer
import com.anrstudio.ads.admob.Admob
import com.anrstudio.ads.ads.ANRAdSdk
import com.anrstudio.ads.funtion.AdCallback
import com.bumptech.glide.Glide
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdRemoteConfig
import com.hyper.castwave.ads.AdUnitConfig
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.ads.AdsManager.loadNativeLanguage
import com.hyper.castwave.ads.RemoteConfigUtils
import com.hyper.castwave.ads.banner_splash
import com.hyper.castwave.ads.inter_splash
import com.hyper.castwave.app.AppConstants
import com.hyper.castwave.app.GlobalApp
import com.hyper.castwave.ui.bases.BannerConfig
import com.hyper.castwave.ui.bases.BaseActivityWithBanner
import com.hyper.castwave.ui.bases.ConsentHandler
import com.hyper.castwave.ui.bases.StatusBarConfig
import com.hyper.castwave.ui.bases.ext.goneView
import com.hyper.castwave.ui.bases.ext.isNetwork
import com.hyper.castwave.utils.Routes
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseActivityWithBanner<ActivitySplashBinding>(), RemoteConfigUtils.Listener {

    override var bannerConfig: BannerConfig = BannerConfig(
        adUnitConfig = AdUnitConfig(
            id = "",
            isEnable = false,
            reloadIntervalSeconds = 0
        ),
        isCollapse = false
    )

    override val statusBarConfig: StatusBarConfig
        get() = StatusBarConfig(
            applyPadding = false
        )

    private var getConfigSuccess = false
    private lateinit var consentHandler: ConsentHandler

    override fun getLayoutActivity() = R.layout.activity_splash

    override fun initViews() {
        super.initViews()
        Glide.with(this).load(R.drawable.bg_splash).centerCrop().into(mBinding.imgBg)
        AdsManager.clearAll()
        RemoteConfigUtils.init(this, this)
        consentHandler = ConsentHandler(
            activity = this,
            appSharedPref = appSharedPref,
            trackingSuffix = 1,
            onConsentFlowCompleted = { loadingRemoteConfig() }
        )
        if (appSharedPref.isConfirmConsent.not() && appSharedPref.isUserGlobal.not() && isNetwork()) {
            consentHandler.requestConsent()
        } else {
            loadingRemoteConfig()
        }

    }


    private fun loadingRemoteConfig() {
        val totalTime = AppConstants.DEFAULT_TIME_SPLASH
        mBinding.progressSplash.max = 100
        mBinding.progressSplash.progress = 0
        object : CountDownTimer(totalTime, 100) {
            override fun onTick(millisUntilFinished: Long) {
                val elapsed = totalTime - millisUntilFinished
                mBinding.progressSplash.progress = (elapsed * 100 / totalTime).toInt()
                if (getConfigSuccess && millisUntilFinished < AppConstants.DEFAULT_LIMIT_TIME_SPLASH) {
                    checkRemoteConfigResult()
                    cancel()
                }
            }

            override fun onFinish() {
                mBinding.progressSplash.progress = 100
                if (!getConfigSuccess) {
                    checkRemoteConfigResult()
                }
            }
        }.start()
    }


    private fun checkRemoteConfigResult() {
        (application as? GlobalApp)?.applyInterstitialInterval(RemoteConfigUtils.getInterInterval())

        if (AdRemoteConfig.banner_splash.isEnable) {
            bannerConfig = BannerConfig(
                AdRemoteConfig.banner_splash,
                false
            )
            loadBanner()
        } else {
            mBinding.frBanner.goneView()
        }
        AdRemoteConfig.initialize(this, RemoteConfigUtils.getAdRemoteConfig())
        loadNativeLanguage(this, R.layout.layout_native_language)

        if (AdRemoteConfig.inter_splash.isEnable && isNetwork(this@SplashActivity)) {
            Admob.getInstance().setOpenActivityAfterShowInterAds(false)
            ANRAdSdk.getInstance().loadSplashInterstitialAds(
                this,
                AdRemoteConfig.inter_splash.id,
                30000,
                5000,
                object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        moveActivity()
                    }
                })
        } else {
            moveActivity()
        }

    }


    private fun moveActivity() {
        Routes.startLanguageActivity(this, null)
        finish()
    }


    override fun onResume() {
        super.onResume()
        ANRAdSdk.getInstance()
            .onCheckShowSplashWhenFail(this@SplashActivity, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    moveActivity()
                }
            }, 1000)
    }

    override fun loadSuccess() {
        getConfigSuccess = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::consentHandler.isInitialized) {
            consentHandler.clear()
        }
    }
}

package com.anrstudio.template.ui.component.splash

import android.annotation.SuppressLint
import android.os.CountDownTimer
import com.anrstudio.ads.admob.Admob
import com.anrstudio.ads.ads.ANRAdSdk
import com.anrstudio.ads.funtion.AdCallback
import com.anrstudio.template.R
import com.anrstudio.template.ads.AdRemoteConfig
import com.anrstudio.template.ads.AdUnitConfig
import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.AdsManager.loadNativeLanguage
import com.anrstudio.template.ads.RemoteConfigUtils
import com.anrstudio.template.ads.banner_splash
import com.anrstudio.template.ads.inter_splash
import com.anrstudio.template.app.AppConstants
import com.anrstudio.template.app.GlobalApp
import com.anrstudio.template.databinding.ActivitySplashBinding
import com.anrstudio.template.ui.bases.BannerConfig
import com.anrstudio.template.ui.bases.BaseActivityWithBanner
import com.anrstudio.template.ui.bases.ConsentHandler
import com.anrstudio.template.ui.bases.StatusBarConfig
import com.anrstudio.template.ui.bases.ext.goneView
import com.anrstudio.template.ui.bases.ext.isNetwork
import com.anrstudio.template.utils.Routes
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
        loadNativeLanguage(this, appSharedPref.firstLanguage, R.layout.layout_native_language)

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

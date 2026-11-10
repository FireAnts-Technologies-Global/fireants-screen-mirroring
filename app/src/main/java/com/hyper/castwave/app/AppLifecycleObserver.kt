package com.hyper.castwave.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.anrstudio.ads.admob.AppOpenManager
import com.anrstudio.ads.ads.ANRAdSdk
import com.anrstudio.ads.billing.AppPurchase
import com.hyper.castwave.ui.component.language.LanguageActivity
import com.hyper.castwave.ui.component.onboarding.OnBoardingActivity
import com.hyper.castwave.ui.component.splash.SplashActivity
import com.hyper.castwave.ui.component.welcome.WelcomeActivity
import com.hyper.castwave.utils.Routes

class AppLifecycleObserver : DefaultLifecycleObserver {

    private val listActivityDisableResume = arrayListOf(
        SplashActivity::class.java,
        LanguageActivity::class.java,
        OnBoardingActivity::class.java,
        WelcomeActivity::class.java,
    )

    override fun onStart(owner: LifecycleOwner) {
        val currentActivity = GlobalApp.currentActivity
        if (currentActivity != null) {
            val isDisable = listActivityDisableResume.any { clazz ->
                clazz.isInstance(currentActivity)
            }
            if (!isDisable && ResumeAdsEntryRule.shouldShowWelcomeOnResume() && !AppOpenManager.getInstance().isInterstitialShowing && !AppPurchase.getInstance()
                    .isPurchased(currentActivity.applicationContext) && ANRAdSdk.getInstance()
                    .shouldDisplayInterWelcomeBack
            ) {
                Routes.startWelcomeActivity(currentActivity)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {}
}
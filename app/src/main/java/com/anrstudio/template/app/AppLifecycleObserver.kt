package com.anrstudio.template.app

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.anrstudio.ads.admob.AppOpenManager
import com.anrstudio.ads.ads.ANRAdSdk
import com.anrstudio.ads.billing.AppPurchase
import com.anrstudio.template.ui.component.language.LanguageActivity
import com.anrstudio.template.ui.component.onboarding.OnBoardingActivity
import com.anrstudio.template.ui.component.splash.SplashActivity
import com.anrstudio.template.ui.component.uninstall.SurveyActivity
import com.anrstudio.template.ui.component.welcome.WelcomeActivity
import com.anrstudio.template.utils.Routes

class AppLifecycleObserver : DefaultLifecycleObserver {

    private val listActivityDisableResume = arrayListOf(
        SplashActivity::class.java,
        LanguageActivity::class.java,
        OnBoardingActivity::class.java,
        WelcomeActivity::class.java,
        SurveyActivity::class.java,
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
package com.anrstudio.template.utils

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.anrstudio.template.app.AppConstants
import com.anrstudio.template.ui.component.language.LanguageActivity
import com.anrstudio.template.ui.component.main.MainActivity
import com.anrstudio.template.ui.component.onboarding.OnBoardingActivity
import com.anrstudio.template.ui.component.setting.SettingActivity
import com.anrstudio.template.ui.component.splash.SplashActivity
import com.anrstudio.template.ui.component.uninstall.SurveyActivity
import com.anrstudio.template.ui.component.welcome.WelcomeActivity

object Routes {
    fun startMainActivity(fromActivity: Activity) =
        Intent(fromActivity, MainActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
            fromActivity.applyOpenTransition()
        }

    fun startWelcomeActivity(fromActivity: Activity) {
        if (fromActivity is WelcomeActivity) return
        val intent = Intent(fromActivity, WelcomeActivity::class.java)
        fromActivity.startActivity(intent)
    }

    fun startOnBoardingActivity(fromActivity: Activity) =
        Intent(fromActivity, OnBoardingActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
            fromActivity.applyOpenTransition()
        }

    fun startLanguageActivity(fromActivity: Activity, bundle: Bundle?) =
        Intent(fromActivity, LanguageActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            bundle?.let { putExtras(it) }
            fromActivity.startActivity(this)
            fromActivity.applyOpenTransition()
        }

    fun startSplashActivity(fromActivity: Activity) =
        Intent(fromActivity, SplashActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
        }

    fun startSurveyActivity(fromActivity: Activity) =
        Intent(fromActivity, SurveyActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
            fromActivity.applyOpenTransition()
        }

    fun startSettingActivity(fromActivity: Activity) =
        Intent(fromActivity, SettingActivity::class.java).apply {
            putExtra(AppConstants.KEY_TRACKING_SCREEN_FROM, fromActivity::class.java.simpleName)
            fromActivity.startActivity(this)
            fromActivity.applyOpenTransition()
        }

    fun addTrackingMoveScreen(fromActivity: String, toActivity: String) {
        ANRTrackingHelper.fromScreenToScreen(fromActivity, toActivity)
    }

}

fun Activity.applyOpenTransition() {
//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left)
//    } else {
//        @Suppress("DEPRECATION")
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
//    }
}
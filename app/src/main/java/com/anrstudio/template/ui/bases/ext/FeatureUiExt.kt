package com.anrstudio.template.ui.bases.ext
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View
import android.animation.*
import android.os.SystemClock
import android.view.animation.OvershootInterpolator
import com.anrstudio.template.app.CastFeatureConstants
import com.anrstudio.template.utils.EasyPreferences
import com.anrstudio.template.utils.EasyPreferences.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
fun Context.openSettingWifi() { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
fun <T> CoroutineScope.observeFlow(flow: Flow<T>, action: (T) -> Unit) { launch { flow.collect(action) } }
fun View.animateClick(debounceMs: Long = 300L, peakScale: Float = 1.06f, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickTime < debounceMs) return@setOnClickListener
        lastClickTime = now
        AnimatorSet().apply {
            playTogether(ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, peakScale, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, peakScale, 1f))
            duration = 150L
            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { action(view) }
            })
            start()
        }
    }
}

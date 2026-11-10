package com.hyper.castwave.ui.bases.ext
import com.hyper.castwave.ui.bases.ext.showToastById
import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.databinding.ViewDataBinding
import com.hyper.castwave.R
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.hyper.castwave.app.CastFeatureConstants
import com.hyper.castwave.ui.component.dialog.CastRateDialog
import com.hyper.castwave.utils.EasyPreferences
import com.hyper.castwave.utils.EasyPreferences.set
fun showCastRateDialog(
    activity: Activity, isFinish: Boolean
) {
    val dialog = CastRateDialog(activity, onRating = {
        val manager: ReviewManager = ReviewManagerFactory.create(activity)
        val request: Task<ReviewInfo> = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                val flow: Task<Void> = manager.launchReviewFlow(
                    activity, reviewInfo
                )
                flow.addOnSuccessListener {
                    EasyPreferences.defaultPrefs(activity)[CastFeatureConstants.KEY_SET_SHOW_DIALOG_RATE] =
                        true
                    if (isFinish) {
                        activity.showToastById(R.string.cast_txt_thanks_you_for_rating)
                        activity.finishAffinity()
                        activity.finish()
                    }
                }
            } else {
                if (isFinish) {
                    activity.finishAffinity()
                    activity.finish()
                }
            }
        }
    }, onNormalRate = {
        activity.showToastById(R.string.cast_txt_thanks_you_for_rating)
    })
    try {
        dialog.show()
    } catch (e: WindowManager.BadTokenException) {
        e.printStackTrace()
    }
}

fun Activity.maybeShowRateAfterFirstWebMirror() {
    val prefs = EasyPreferences.defaultPrefs(this)
    if (prefs.getBoolean(CastFeatureConstants.KEY_WEB_MIRROR_RATE_SUGGESTED, false)) return
    if (prefs.getBoolean(CastFeatureConstants.KEY_SET_SHOW_DIALOG_RATE, false)) return
    prefs[CastFeatureConstants.KEY_WEB_MIRROR_RATE_SUGGESTED] = true
    showCastRateDialog(this, isFinish = false)
}

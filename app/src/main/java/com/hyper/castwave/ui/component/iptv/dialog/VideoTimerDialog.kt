package com.hyper.castwave.ui.component.iptv.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import com.hyper.castwave.R
import com.hyper.castwave.data.iptv.pref.AppSharedPref
import com.hyper.castwave.databinding.CastDialogVideoTimerBinding
import com.hyper.castwave.ui.bases.BaseDialogFragment
import com.hyper.castwave.ui.bases.ext.click
import com.hyper.castwave.ui.bases.ext.goneView
import com.hyper.castwave.ui.bases.ext.visibleView
import com.hyper.castwave.ui.component.iptv.util.CalendarUtil.TIME_ONE_SECOND
import com.hyper.castwave.ui.component.iptv.util.CalendarUtil.getDistanceTimeFromTo
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject
import androidx.core.graphics.drawable.toDrawable

@AndroidEntryPoint
class VideoTimerDialog : BaseDialogFragment<CastDialogVideoTimerBinding>() {

    override fun getLayoutDialog() = R.layout.cast_dialog_video_timer

    companion object {

        const val TAG_VIDEO_TIMER_DIALOG = "tag_video_timer_dialog"

        fun newInstance() = VideoTimerDialog()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        }
    }
    @Inject
    lateinit var appSharedPref: AppSharedPref

    private var listener: VideoTimerDialogListener? = null

    private var hour = 0

    private var minute = 0

    private var handler: Handler? = null

    private val runnable = object : Runnable {

        override fun run() {
            val currentTime = System.currentTimeMillis()
            val timeSet = appSharedPref.timeNeedPauseVideo
            if (timeSet > currentTime) {
                val hourAndMinute = getDistanceTimeFromTo(currentTime, timeSet)
                mBinding.tvDes.text = getString(R.string.cast_sleep_timer_value, hourAndMinute.first, hourAndMinute.second, hourAndMinute.third)
                handler?.postDelayed(this, TIME_ONE_SECOND)
            } else handler?.removeCallbacksAndMessages(null)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as VideoTimerDialogListener
        } catch (_: Exception) {
            try {
                listener = context as VideoTimerDialogListener
            } catch (_: Exception) {
            }
        }
    }

    override fun initViews() {
        super.initViews()
        val calendar = Calendar.getInstance()

        val currentTime = System.currentTimeMillis()
        val timeSet = appSharedPref.timeNeedPauseVideo
        if (timeSet > currentTime) {
            handler = Handler(Looper.getMainLooper())
            val hourAndMinute = getDistanceTimeFromTo(currentTime, timeSet)
            mBinding.tvDes.text = getString(R.string.cast_sleep_timer_value, hourAndMinute.first, hourAndMinute.second, hourAndMinute.third)
            handler?.postDelayed(runnable, TIME_ONE_SECOND)
            mBinding.tvDes.visibleView()
            mBinding.btnClear.visibleView()
            mBinding.btnSet.goneView()

            calendar.timeInMillis = timeSet
        } else {
            mBinding.tvDes.goneView()
            mBinding.btnClear.goneView()
            mBinding.btnSet.visibleView()
        }

        hour = calendar.get(Calendar.HOUR_OF_DAY)
        minute = calendar.get(Calendar.MINUTE)
        mBinding.npHour.value = hour
        mBinding.npMinute.value = minute
    }

    override fun onClickViews() {
        super.onClickViews()

        mBinding.btnClose.click {
            dismiss()
        }

        mBinding.btnSet.click {
            listener?.onSetTime(mBinding.npHour.value, mBinding.npMinute.value)
            dismiss()
        }

        mBinding.btnClear.click {
            listener?.onClearTime()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler?.removeCallbacksAndMessages(null)
    }

    interface VideoTimerDialogListener {

        fun onSetTime(hour: Int, minute: Int)

        fun onClearTime()
    }
}

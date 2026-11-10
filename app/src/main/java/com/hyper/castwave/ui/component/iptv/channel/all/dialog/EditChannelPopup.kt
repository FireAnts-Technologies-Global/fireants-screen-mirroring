package com.hyper.castwave.ui.component.iptv.channel.all.dialog

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.databinding.DataBindingUtil
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastLayoutEditChannelBinding
import com.hyper.castwave.data.iptv.model.TVChannel
import androidx.core.graphics.drawable.toDrawable

class EditChannelPopup(
    context: Context,
    private val channel: TVChannel,
    private val onEditClick: (TVChannel) -> Unit,
    private val onDeleteClick: (TVChannel) -> Unit,
) {
    private var popupWindow: PopupWindow? = null
    private val binding: CastLayoutEditChannelBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.cast_layout_edit_channel,
        null,
        false
    )

    init {
        setupPopupWindow()
        setupClickListeners()
    }

    private fun setupPopupWindow() {
        popupWindow = PopupWindow(
            binding.root,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            elevation = 8f
        }
    }

    private fun setupClickListeners() {
        binding.layoutEdit.setOnClickListener {
            onEditClick(channel)
            dismiss()
        }

        binding.layoutDelete.setOnClickListener {
            onDeleteClick(channel)
            dismiss()
        }
    }

    fun show(anchorView: View) {
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val anchorWidth = anchorView.width
        val popupWidth = binding.root.measuredWidth
        val xOffset = anchorWidth - popupWidth

        val gap = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            4f,
            anchorView.context.resources.displayMetrics
        ).toInt()

        popupWindow?.showAsDropDown(anchorView, xOffset, gap, Gravity.END)
    }

    fun dismiss() {
        popupWindow?.dismiss()
        popupWindow = null
    }

    fun isShowing(): Boolean {
        return popupWindow?.isShowing == true
    }
}


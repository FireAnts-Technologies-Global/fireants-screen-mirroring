package com.anrstudio.template.ui.bases

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

abstract class BaseDialogFragment<VB : ViewDataBinding> : DialogFragment() {

    abstract fun getLayoutDialog(): Int

    lateinit var mBinding: VB


    open val fullScreen = false

    open val isTransparentBackground: Boolean = true

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(context), getLayoutDialog(), null, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        onResizeViews()
        onClickViews()
        observerData()
    }

    open fun initViews() {}

    open fun onResizeViews() {}

    open fun onClickViews() {}

    open fun observerData() {}

    override fun onStart() {
        super.onStart()
        if (isTransparentBackground) {
            dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        if (fullScreen) {
            dialog?.window?.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            )
        } else {
            dialog?.window?.setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            if (manager.findFragmentByTag(tag) == null) {
                super.show(manager, tag)
            }
        } catch (_: Exception) {
        }
    }
}

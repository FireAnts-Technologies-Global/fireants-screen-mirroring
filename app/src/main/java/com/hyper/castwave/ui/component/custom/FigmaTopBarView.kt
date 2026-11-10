package com.hyper.castwave.ui.component.custom

import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.hyper.castwave.R

/** Shared XML-backed top bar for navigation headers that follow the Figma app-bar patterns. */
class FigmaTopBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    enum class Variant { COMPACT_TITLE, STANDARD, CENTERED, CENTERED_ACTIONS, TRANSPARENT }
    enum class BackStyle { CAST, HOW_TO_USE, IMPORT, MEDIA }

    private val backButton: FrameLayout
    private val backIcon: ImageView
    private val titleView: TextView
    private val endAction: FrameLayout
    private val endIcon: ImageView
    private var currentVariant = Variant.STANDARD
    private var currentBackStyle = BackStyle.CAST

    init {
        LayoutInflater.from(context).inflate(R.layout.cast_view_figma_top_bar_content, this, true)
        backButton = findViewById(R.id.topBarBackButton)
        backIcon = findViewById(R.id.topBarBackIcon)
        titleView = findViewById(R.id.topBarTitle)
        endAction = findViewById(R.id.topBarEndAction)
        endIcon = findViewById(R.id.topBarEndIcon)

        val a = context.obtainStyledAttributes(attrs, R.styleable.FigmaTopBarView, defStyleAttr, 0)
        val variant = a.getInt(R.styleable.FigmaTopBarView_figmaTopBarVariant, 1)
        val backStyle = a.getInt(R.styleable.FigmaTopBarView_figmaTopBarBackStyle, 0)
        val title = a.getString(R.styleable.FigmaTopBarView_figmaTopBarTitle)
        val backDescription = a.getString(R.styleable.FigmaTopBarView_figmaTopBarBackDescription)
        val iconRes = a.getResourceId(R.styleable.FigmaTopBarView_figmaTopBarBackIcon, 0)
        a.recycle()

        currentBackStyle = when (backStyle) {
            1 -> BackStyle.HOW_TO_USE
            2 -> BackStyle.IMPORT
            3 -> BackStyle.MEDIA
            else -> BackStyle.CAST
        }
        titleView.text = title.orEmpty()
        if (!backDescription.isNullOrBlank()) backButton.contentDescription = backDescription
        if (iconRes != 0) backIcon.setImageResource(iconRes)
        setVariant(Variant.values().getOrElse(variant) { Variant.STANDARD })
    }

    fun setVariant(variant: Variant) {
        currentVariant = variant
        val hasBack = variant != Variant.COMPACT_TITLE
        backButton.visibility = if (hasBack) VISIBLE else GONE
        endAction.visibility =
            if (variant == Variant.CENTERED_ACTIONS) VISIBLE else INVISIBLE
        val backBackground = when (currentBackStyle) {
            BackStyle.HOW_TO_USE -> R.drawable.cast_how_to_use_back_light
            BackStyle.IMPORT -> R.drawable.cast_bg_import_back
            BackStyle.MEDIA -> R.drawable.cast_bg_media_back
            BackStyle.CAST -> R.drawable.cast_bg_cast_toolbar_button
        }
        val backIconResource = when (currentBackStyle) {
            BackStyle.HOW_TO_USE -> R.drawable.cast_how_to_use_icon_back
            BackStyle.IMPORT -> R.drawable.cast_ic_how_to_use_back_arrow
            BackStyle.MEDIA -> R.drawable.cast_ic_figma_back
            BackStyle.CAST -> R.drawable.cast_ic_figma_back
        }
        backButton.background =
            ResourcesCompat.getDrawable(resources, backBackground, context.theme)
        backIcon.setImageResource(backIconResource)
        backIcon.imageTintList = if (currentBackStyle == BackStyle.IMPORT) {
            android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(
                    context,
                    R.color.cast_import_blue
                )
            )
        } else null

        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.cast_cast_space_16)
        val verticalPadding =
            if (variant == Variant.CENTERED || variant == Variant.CENTERED_ACTIONS) {
                resources.getDimensionPixelSize(R.dimen.cast_cast_space_8)
            } else 0
        setPaddingRelative(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        titleView.textSize = when {
            currentBackStyle == BackStyle.HOW_TO_USE -> 19f
            variant == Variant.CENTERED || variant == Variant.CENTERED_ACTIONS -> 22f
            else -> 20f
        }
        titleView.typeface = ResourcesCompat.getFont(
            context,
            if (currentBackStyle == BackStyle.HOW_TO_USE) R.font.inter_bold else R.font.inter_bold,
        )
        titleView.gravity =
            if (variant == Variant.CENTERED || variant == Variant.CENTERED_ACTIONS) {
                android.view.Gravity.CENTER
            } else {
                android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            }

        val titleParams = titleView.layoutParams as LayoutParams
        val compactTitle = variant == Variant.COMPACT_TITLE
        titleParams.startToStart = if (hasBack) LayoutParams.UNSET else LayoutParams.PARENT_ID
        titleParams.startToEnd = if (hasBack) backButton.id else LayoutParams.UNSET
        titleParams.endToStart = if (compactTitle) LayoutParams.UNSET else endAction.id
        titleParams.endToEnd = if (compactTitle) LayoutParams.PARENT_ID else LayoutParams.UNSET
        titleParams.marginStart = if (variant == Variant.STANDARD) {
            resources.getDimensionPixelSize(R.dimen.cast_cast_space_12)
        } else 0
        titleView.layoutParams = titleParams
        requestLayout()
    }

    fun setTitle(title: CharSequence?) {
        titleView.text = title
    }

    fun setBackContentDescription(description: CharSequence?) {
        backButton.contentDescription = description
    }

    fun setOnBackClickListener(listener: OnClickListener?) {
        setOnBackClickListener(animated = false, listener = listener)
    }

    fun setOnBackClickListener(animated: Boolean, listener: OnClickListener?) {
        if (!animated || listener == null) {
            backButton.setOnClickListener(listener)
            return
        }
        var lastClickTime = 0L
        backButton.setOnClickListener { view ->
            val now = SystemClock.elapsedRealtime()
            if (now - lastClickTime < 300L) return@setOnClickListener
            lastClickTime = now
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(view, SCALE_X, 1f, 1.06f, 1f),
                    ObjectAnimator.ofFloat(view, SCALE_Y, 1f, 1.06f, 1f),
                )
                duration = 150L
                interpolator = OvershootInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        listener.onClick(view)
                    }
                })
                start()
            }
        }
    }

    fun setEndAction(
        @DrawableRes iconRes: Int,
        contentDescription: CharSequence?,
        listener: OnClickListener?,
        @DrawableRes backgroundRes: Int = 0,
    ) {
        endAction.visibility = VISIBLE
        endAction.background = if (backgroundRes == 0) null else ResourcesCompat.getDrawable(
            resources,
            backgroundRes,
            context.theme,
        )
        endAction.isClickable = listener != null
        endAction.isFocusable = listener != null
        endAction.contentDescription = contentDescription
        endIcon.visibility = VISIBLE
        endIcon.setImageResource(iconRes)
        endAction.setOnClickListener(listener)
        if (currentVariant != Variant.CENTERED_ACTIONS) setVariant(Variant.CENTERED_ACTIONS)
    }

    fun setBackIconSize(sizePx: Int) {
        backIcon.layoutParams = backIcon.layoutParams.apply {
            width = sizePx
            height = sizePx
        }
    }

    fun animateEndActionIconBy(degrees: Float, durationMillis: Long) {
        endIcon.animate().cancel()
        endIcon.rotation = 0f
        endIcon.animate()
            .rotationBy(degrees)
            .setDuration(durationMillis)
            .setInterpolator(LinearInterpolator())
            .start()
    }

    fun setEndActionIcon(@DrawableRes iconRes: Int) {
        endIcon.setImageResource(iconRes)
    }

    fun clearEndAction() {
        endAction.setOnClickListener(null)
        endAction.isClickable = false
        endAction.isFocusable = false
        endAction.contentDescription = null
        endIcon.setImageDrawable(null)
        endIcon.visibility = GONE
        endAction.visibility = INVISIBLE
    }
}

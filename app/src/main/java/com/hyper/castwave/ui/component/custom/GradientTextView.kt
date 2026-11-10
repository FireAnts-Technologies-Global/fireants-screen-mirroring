package com.hyper.castwave.ui.component.custom

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import com.hyper.castwave.R

class GradientTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var startColor: Int = ContextCompat.getColor(context, R.color.cast_color_51A2FE)
    private var endColor: Int = ContextCompat.getColor(context, R.color.cast_color_0452F7)

    init {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.GradientTextView) {
                startColor = getColor(R.styleable.GradientTextView_gradientStartColor, startColor)
                endColor = getColor(R.styleable.GradientTextView_gradientEndColor, endColor)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGradient()
    }

    private fun updateGradient() {
        if (width == 0 || height == 0) return
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            startColor,
            endColor,
            Shader.TileMode.CLAMP
        )
        invalidate()
    }

    fun setGradientColors(@ColorRes startColorRes: Int, @ColorRes endColorRes: Int) {
        this.startColor = ContextCompat.getColor(context, startColorRes)
        this.endColor = ContextCompat.getColor(context, endColorRes)
        updateGradient()
    }
}

package com.anrstudio.template.ui.component.custom

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import com.anrstudio.template.R

class CustomRatingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var currentRating = 0
    private var listener: OnRatingChangeListener? = null
    private val starViews = mutableListOf<ImageView>()

    interface OnRatingChangeListener {
        fun onRatingChanged(ratingBar: CustomRatingBar, rating: Int)
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        // Tạo 5 sao
        for (i in 0 until 5) {
            val starView = ImageView(context).apply {
                // Mỗi sao chiếm 1/5 chiều ngang (weight = 1), tự fit vào phần của nó
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                setImageResource(R.drawable.cast_start_unselect)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                adjustViewBounds = true
                setPadding(0, 0, 0, 0)

                // Click listener cho mỗi sao
                setOnClickListener {
                    val newRating = i + 1
                    // Nếu click vào sao đã được chọn, reset về 0
                    if (currentRating == newRating) {
                        setRating(0, true)
                    } else {
                        setRating(newRating, true)
                    }
                }
            }
            starViews.add(starView)
            addView(starView)
        }

        setRating(0, false)
    }

    fun setRating(rating: Int, notifyListener: Boolean = false) {
        if (rating < 0 || rating > 5) return

        currentRating = rating

        // Cập nhật hình ảnh cho từng sao
        starViews.forEachIndexed { index, starView ->
            if (index < rating) {
                starView.setImageResource(R.drawable.cast_start_selected)
            } else {
                starView.setImageResource(R.drawable.cast_start_unselect)
            }
        }

        if (notifyListener) {
            listener?.onRatingChanged(this, rating)
        }
    }

    fun getRating(): Int = currentRating

    fun setOnRatingChangeListener(listener: OnRatingChangeListener?) {
        this.listener = listener
    }
}


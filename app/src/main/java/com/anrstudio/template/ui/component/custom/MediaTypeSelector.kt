package com.anrstudio.template.ui.component.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastViewMediaTypeSelectorBinding
import com.anrstudio.template.domain.model.MediaType

class MediaTypeSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: CastViewMediaTypeSelectorBinding =
        CastViewMediaTypeSelectorBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedType: MediaType = MediaType.VIDEO
    private var onTypeSelectedListener: ((MediaType) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        setupClicks()
    }

    private fun setupClicks() {
        binding.btnVideo.setOnClickListener {
            selectType(MediaType.VIDEO)
        }
        binding.btnImage.setOnClickListener {
            selectType(MediaType.IMAGE)
        }
        binding.btnAudio.setOnClickListener {
            selectType(MediaType.AUDIO)
        }
    }

    fun setSelectedType(type: MediaType) {
        selectedType = type
        updateUI()
    }

    fun setOnTypeSelectedListener(listener: (MediaType) -> Unit) {
        onTypeSelectedListener = listener
    }

    private fun selectType(type: MediaType) {
        if (selectedType != type) {
            selectedType = type
            updateUI()
            onTypeSelectedListener?.invoke(type)
        }
    }

    private fun updateUI() {
        val selectedColor = ContextCompat.getColor(context, R.color.cast_main_color_ad)
        val unselectedColor = ContextCompat.getColor(context, R.color.cast_gray_text)

        binding.btnVideo.apply {
            setTextColor(if (selectedType == MediaType.VIDEO) selectedColor else unselectedColor)

        }
        binding.btnImage.apply {
            setTextColor(if (selectedType == MediaType.IMAGE) selectedColor else unselectedColor)
        }
        binding.btnAudio.apply {
            setTextColor(if (selectedType == MediaType.AUDIO) selectedColor else unselectedColor)
        }
    }
}

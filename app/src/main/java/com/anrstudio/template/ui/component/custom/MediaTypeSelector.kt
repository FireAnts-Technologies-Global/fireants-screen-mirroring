package com.anrstudio.template.ui.component.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.view.View
import androidx.core.content.ContextCompat
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastViewMediaTypeSelectorBinding
import com.anrstudio.template.domain.model.MediaType
import com.anrstudio.template.domain.model.MediaTab

class MediaTypeSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: CastViewMediaTypeSelectorBinding =
        CastViewMediaTypeSelectorBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedType: MediaType = MediaType.VIDEO
    private var selectedTab: MediaTab = MediaTab.VIDEOS
    private var onTypeSelectedListener: ((MediaType) -> Unit)? = null
    private var onTabSelectedListener: ((MediaTab) -> Unit)? = null

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
        binding.btnImage.setOnClickListener { selectTab(MediaTab.PHOTOS) }
        binding.btnVideo.setOnClickListener { selectTab(MediaTab.VIDEOS) }
        binding.btnRecent.setOnClickListener { selectTab(MediaTab.RECENTLY) }
        binding.btnFavorite.setOnClickListener { selectTab(MediaTab.FAVORITE) }
        binding.btnPortrait.setOnClickListener { selectTab(MediaTab.PORTRAIT) }
    }

    fun setSelectedType(type: MediaType) {
        selectedType = type
        selectedTab = when (type) {
            MediaType.IMAGE -> MediaTab.PHOTOS
            MediaType.VIDEO -> MediaTab.VIDEOS
            MediaType.AUDIO -> MediaTab.AUDIO
        }
        updateUI()
    }

    fun setSelectedTab(tab: MediaTab) {
        selectedTab = tab
        selectedType = when (tab) {
            MediaTab.VIDEOS -> MediaType.VIDEO
            MediaTab.AUDIO -> MediaType.AUDIO
            else -> MediaType.IMAGE
        }
        updateUI()
    }

    fun setOnTypeSelectedListener(listener: (MediaType) -> Unit) {
        onTypeSelectedListener = listener
    }

    fun setOnTabSelectedListener(listener: (MediaTab) -> Unit) {
        onTabSelectedListener = listener
    }

    private fun selectType(type: MediaType) {
        val tab = when (type) {
            MediaType.IMAGE -> MediaTab.PHOTOS
            MediaType.VIDEO -> MediaTab.VIDEOS
            MediaType.AUDIO -> MediaTab.AUDIO
        }
        if (selectedType != type || selectedTab != tab) selectTab(tab)
        onTypeSelectedListener?.invoke(type)
    }

    private fun selectTab(tab: MediaTab) {
        if (selectedTab == tab) return
        selectedTab = tab
        selectedType = when (tab) {
            MediaTab.VIDEOS -> MediaType.VIDEO
            MediaTab.AUDIO -> MediaType.AUDIO
            else -> MediaType.IMAGE
        }
        updateUI()
        onTabSelectedListener?.invoke(tab)
        if (tab == MediaTab.PHOTOS || tab == MediaTab.VIDEOS || tab == MediaTab.AUDIO) {
            onTypeSelectedListener?.invoke(selectedType)
        }
    }

    private fun updateUI() {
        val selectedColor = ContextCompat.getColor(context, R.color.cast_cast_accent)
        val unselectedColor = ContextCompat.getColor(context, R.color.cast_cast_secondary_text)
        val tabs = listOf(
            MediaTab.PHOTOS to binding.btnImage,
            MediaTab.VIDEOS to binding.btnVideo,
            MediaTab.RECENTLY to binding.btnRecent,
            MediaTab.FAVORITE to binding.btnFavorite,
            MediaTab.PORTRAIT to binding.btnPortrait,
            MediaTab.AUDIO to binding.btnAudio,
        )
        binding.btnAudio.visibility = if (selectedTab == MediaTab.AUDIO) View.VISIBLE else View.GONE
        tabs.forEach { (tab, view) ->
            val selected = selectedTab == tab
            view.setTextColor(if (selected) selectedColor else unselectedColor)
            view.typeface = androidx.core.content.res.ResourcesCompat.getFont(
                context,
                if (selected) R.font.inter_semibold else R.font.inter_regular,
            )
            view.setBackgroundResource(
                if (selected) R.drawable.cast_bg_media_nav_item_selected else android.R.color.transparent
            )
        }
    }
}

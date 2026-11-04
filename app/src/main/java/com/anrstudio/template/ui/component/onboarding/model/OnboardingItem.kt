package com.anrstudio.template.ui.component.onboarding.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.anrstudio.template.R
import kotlinx.parcelize.Parcelize

enum class NativeFullPlacement {
    AFTER_PAGE_1,
    AFTER_PAGE_2
}

@Parcelize
data class OnboardingItem(
    @StringRes val title: Int = R.string.onboarding_title_1,
    @StringRes val description: Int = R.string.onboarding_title_1,
    @StringRes val textButton: Int = R.string.next,
    @DrawableRes val imageResId: Int = R.drawable.ic_vietnamese,
    val positionIndicator: Int = -1,
    val isHasNativeOnPage1: Boolean = false,
    val isHasNativeOnPage4: Boolean = false,
    val nativeFullPlacement: NativeFullPlacement? = null,
) : Parcelable

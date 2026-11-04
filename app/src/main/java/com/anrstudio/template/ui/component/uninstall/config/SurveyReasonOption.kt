package com.anrstudio.template.ui.component.uninstall.config

import androidx.annotation.StringRes

data class SurveyReasonOption(
    val id: String,
    @StringRes val labelRes: Int,
    val defaultChecked: Boolean = false,
)

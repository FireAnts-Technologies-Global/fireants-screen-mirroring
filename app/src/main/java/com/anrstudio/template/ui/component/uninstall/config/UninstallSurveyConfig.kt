package com.anrstudio.template.ui.component.uninstall.config

import androidx.annotation.StringRes
import com.anrstudio.template.R

object UninstallSurveyConfig {

    @StringRes
    val sectionTitleRes: Int = R.string.issues_encountered_during_usage

    val reasonOptions: List<SurveyReasonOption> = listOf(
        SurveyReasonOption(
            id = "not_working",
            labelRes = R.string.feature_does_not_working,
            defaultChecked = true,
        ),
        SurveyReasonOption(
            id = "too_many_ads",
            labelRes = R.string.too_many_ads,
        ),
        SurveyReasonOption(
            id = "dont_need",
            labelRes = R.string.i_don_t_need_it,
        ),
        SurveyReasonOption(
            id = "other",
            labelRes = R.string.other,
        ),
    )
}

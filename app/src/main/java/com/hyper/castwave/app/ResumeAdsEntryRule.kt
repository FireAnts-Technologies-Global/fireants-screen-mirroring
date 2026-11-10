package com.hyper.castwave.app

import com.hyper.castwave.ads.AdRemoteConfig
import com.hyper.castwave.ads.inter_welcome
import com.hyper.castwave.ads.native_welcome
import com.hyper.castwave.ads.open_resume

enum class ResumeAdsEntryMode {
    OPEN_RESUME,
    WELCOME,
    NONE,
}

object ResumeAdsEntryRule {
    fun currentMode(): ResumeAdsEntryMode {
        if (!AdRemoteConfig.isInitialized()) return ResumeAdsEntryMode.NONE

        val canUseWelcome =
            AdRemoteConfig.native_welcome.isEnable && AdRemoteConfig.inter_welcome.isEnable
        if (canUseWelcome) return ResumeAdsEntryMode.WELCOME

        val canUseOpenResume = AdRemoteConfig.open_resume.isEnable
        return if (canUseOpenResume) ResumeAdsEntryMode.OPEN_RESUME else ResumeAdsEntryMode.NONE
    }

    fun shouldEnableOpenResume(): Boolean = currentMode() == ResumeAdsEntryMode.OPEN_RESUME

    fun shouldShowWelcomeOnResume(): Boolean = currentMode() == ResumeAdsEntryMode.WELCOME
}

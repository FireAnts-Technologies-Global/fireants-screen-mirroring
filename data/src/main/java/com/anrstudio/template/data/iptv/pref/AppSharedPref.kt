package com.anrstudio.template.data.iptv.pref

import android.content.SharedPreferences

interface AppSharedPref {

    val sharedPref: SharedPreferences

    val editor: SharedPreferences.Editor

    var languageCode: String

    var firstLanguage: Boolean

    var firstOnBoarding: Boolean

    var firstOpen: Boolean

    var isConfirmConsent: Boolean

    var isUserGlobal: Boolean

    var timeNeedPauseVideo: Long

    var isRated: Boolean
}
package com.hyper.castwave.data.iptv.pref

import android.content.Context
import android.content.SharedPreferences

class AppSharedPreferencesApp(context: Context) : AppSharedPref {

    companion object {

        private const val PREFERENCE_FILE_KEY = "app_shared_preferences_app"

        private const val LANGUAGE_CODE = "language_code"

        private const val FIRST_LANGUAGE = "first_language"

        private const val FIRST_ONBOARDING = "first_onboarding"

        private const val FIRST_OPEN = "first_open"

        private const val IS_CONFIRM_CONSENT = "is_confirm_consent"

        private const val IS_USER_GLOBAL = "is_user_global"

        private const val TIME_NEED_PAUSE_VIDEO = "time_need_pause_video"

        private const val IS_RATED = "is_rated"
    }

    override val sharedPref: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_FILE_KEY, Context.MODE_PRIVATE)

    override val editor: SharedPreferences.Editor
        get() = sharedPref.edit()

    override var languageCode: String
        get() = sharedPref.getString(LANGUAGE_CODE, "en") ?: "en"
        set(value) = editor.putString(LANGUAGE_CODE, value).apply()

    override var firstLanguage: Boolean
        get() = sharedPref.getBoolean(FIRST_LANGUAGE, true)
        set(value) = editor.putBoolean(FIRST_LANGUAGE, value).apply()

    override var firstOnBoarding: Boolean
        get() = sharedPref.getBoolean(FIRST_ONBOARDING, true)
        set(value) = editor.putBoolean(FIRST_ONBOARDING, value).apply()

    override var firstOpen: Boolean
        get() = sharedPref.getBoolean(FIRST_OPEN, true)
        set(value) = editor.putBoolean(FIRST_OPEN, value).apply()

    override var isConfirmConsent: Boolean
        get() = sharedPref.getBoolean(IS_CONFIRM_CONSENT, false)
        set(value) = editor.putBoolean(IS_CONFIRM_CONSENT, value).apply()

    override var isUserGlobal: Boolean
        get() = sharedPref.getBoolean(IS_USER_GLOBAL, false)
        set(value) = editor.putBoolean(IS_USER_GLOBAL, value).apply()

    override var timeNeedPauseVideo: Long
        get() = sharedPref.getLong(TIME_NEED_PAUSE_VIDEO, 0L)
        set(value) = editor.putLong(TIME_NEED_PAUSE_VIDEO, value).apply()

    override var isRated: Boolean
        get() = sharedPref.getBoolean(IS_RATED, false)
        set(value) = editor.putBoolean(IS_RATED, value).apply()

}
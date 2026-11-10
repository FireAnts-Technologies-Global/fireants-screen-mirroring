package com.hyper.castwave.ads

import android.content.Context
import com.hyper.castwave.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import timber.log.Timber
import java.io.InputStream

data class AdRemoteConfig(
    val ads: Map<String, AdUnitConfig> = emptyMap()
) {
    companion object {
        private const val RELEASE_FILE_NAME = "ad_config.json"
        private const val DEBUG_FILE_NAME = "ad_config_debug.json"

        @Volatile
        private var instance: AdRemoteConfig? = null

        private val moshi: Moshi = Moshi.Builder()
            .add(AdRemoteConfigJsonAdapterFactory())
            .addLast(KotlinJsonAdapterFactory())
            .build()

        private val adapter = moshi.adapter(AdRemoteConfig::class.java)

        fun getInstance(): AdRemoteConfig {
            return instance ?: synchronized(this) {
                instance
                    ?: throw IllegalStateException("AdRemoteConfig not initialized. Call initialize() first.")
            }
        }

        fun initialize(context: Context, json: String? = null) {
            synchronized(this) {
                instance = if (BuildConfig.DEBUG) {
                    fromAssets(context, DEBUG_FILE_NAME)
                } else {
                    fromJsonOrAssets(context, json, RELEASE_FILE_NAME)
                }
            }
        }

        fun initializeFromAssets(context: Context) {
            synchronized(this) {
                val fileName = if (BuildConfig.DEBUG) DEBUG_FILE_NAME else RELEASE_FILE_NAME
                instance = fromAssets(context, fileName)
            }
        }

        fun initializeFromJson(json: String) {
            synchronized(this) {
                instance = fromJson(json)
            }
        }

        fun updateInstance(newConfig: AdRemoteConfig) {
            synchronized(this) {
                instance = newConfig
            }
        }

        fun isInitialized(): Boolean {
            return instance != null
        }

        fun reset() {
            synchronized(this) {
                instance = null
            }
        }

        private fun fromJson(json: String): AdRemoteConfig {
            return adapter.fromJson(json) ?: throw IllegalArgumentException("Invalid JSON")
        }

        private fun fromInputStream(inputStream: InputStream): AdRemoteConfig {
            return inputStream.bufferedReader().use { reader ->
                adapter.fromJson(reader.readText())
                    ?: throw IllegalArgumentException("Invalid JSON")
            }
        }

        private fun fromAssets(
            context: Context,
            fileName: String = RELEASE_FILE_NAME
        ): AdRemoteConfig {
            return context.assets.open(fileName).use { inputStream ->
                fromInputStream(inputStream)
            }
        }

        fun fromRawResource(context: Context, resId: Int): AdRemoteConfig {
            return context.resources.openRawResource(resId).use { inputStream ->
                fromInputStream(inputStream)
            }
        }

        private fun fromJsonOrAssets(
            context: Context,
            json: String?,
            fileName: String = RELEASE_FILE_NAME
        ): AdRemoteConfig {
            if (!json.isNullOrBlank()) {
                try {
                    return fromJson(json)
                } catch (e: Exception) {
                    // Fallback to assets if JSON parsing fails
                }
            }
            return fromAssets(context, fileName)
        }

        fun fromInputStreamOrAssets(
            context: Context,
            inputStream: InputStream?,
            fileName: String = RELEASE_FILE_NAME
        ): AdRemoteConfig {
            if (BuildConfig.DEBUG) {
                return fromAssets(context, DEBUG_FILE_NAME)
            }
            if (inputStream != null) {
                try {
                    return fromInputStream(inputStream)
                } catch (e: Exception) {
                    // Fallback to assets if InputStream parsing fails
                }
            }
            return fromAssets(context, fileName)
        }
    }

    private fun getAdUnit(key: String): AdUnitConfig {
        val adUnitConfig = ads[key]
        if (adUnitConfig == null) {
            Timber.w("Ad unit '%s' not found in configuration.", key)
            return AdUnitConfig(id = "", isEnable = false)
        }
        return adUnitConfig
    }

    val banner_splash: AdUnitConfig
        get() = getAdUnit("banner_splash")

    val inter_splash: AdUnitConfig
        get() = getAdUnit("inter_splash")

    val native_language_1_1: AdUnitConfig
        get() = getAdUnit("native_language_1.1")

    val native_language_1_2: AdUnitConfig
        get() = getAdUnit("native_language_1.2")

    val native_onboarding_fullscreen_1_1: AdUnitConfig
        get() = getAdUnit("native_onboarding_fullscreen_1.1")

    val native_onboarding_fullscreen_1_2: AdUnitConfig
        get() = getAdUnit("native_onboarding_fullscreen_1.2")

    val native_onboarding_4: AdUnitConfig
        get() = getAdUnit("native_onboarding_4")

    val inter_onboarding_4: AdUnitConfig
        get() = getAdUnit("inter_onboarding_4")

    val inter_welcome_back: AdUnitConfig
        get() = getAdUnit("inter_welcome_back")

    val native_welcome_back: AdUnitConfig
        get() = getAdUnit("native_welcome_back")

    val inter_home: AdUnitConfig
        get() = getAdUnit("inter_home")

    val native_home: AdUnitConfig
        get() = getAdUnit("native_home")

    val inter_back: AdUnitConfig
        get() = getAdUnit("inter_back")

    val banner_searching_devices: AdUnitConfig
        get() = getAdUnit("banner_searching_devices")

    val rewarded_screen_mirroring: AdUnitConfig
        get() = getAdUnit("rewarded_screen_mirroring")

    val banner_cast_image_collage: AdUnitConfig
        get() = getAdUnit("banner_cast_image_collage")

    val banner_cast_video_collage: AdUnitConfig
        get() = getAdUnit("banner_cast_video_collage")

    val banner_channels_collage: AdUnitConfig
        get() = getAdUnit("banner_channels_collage")

    val banner_import_channels_collage: AdUnitConfig
        get() = getAdUnit("banner_import_channels_collage")

    val inter_import_successful_channels: AdUnitConfig
        get() = getAdUnit("inter_import_successful_channels")

    val banner_player: AdUnitConfig
        get() = getAdUnit("banner_player")

    val native_how_to_use: AdUnitConfig
        get() = getAdUnit("native_how_to_use")

    val banner_change_language: AdUnitConfig
        get() = getAdUnit("banner_change_language")
}

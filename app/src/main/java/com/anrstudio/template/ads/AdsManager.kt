package com.anrstudio.template.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.anrstudio.ads.ads.ANRAdSdk
import com.anrstudio.ads.ads.wrapper.ApInterstitialAd
import com.anrstudio.ads.ads.wrapper.ApNativeAd
import com.anrstudio.ads.billing.AppPurchase
import com.anrstudio.ads.funtion.AdCallback
import com.anrstudio.ads.util.AppConstant
import com.anrstudio.template.ui.bases.ext.goneView
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object AdsManager {
    enum class NativeAdLoadState {
        IDLE,
        LOADING,
        LOADED,
        FAILED
    }

    val nativeLanguageAdLive = MutableLiveData<ApNativeAd?>()
    val nativeLanguageClickAdLive = MutableLiveData<ApNativeAd?>()
    val nativeLanguageAdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeLanguageClickAdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeOnboarding1AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding4AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboardingFullAfterPage1AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboardingFullAfterPage3AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding1AdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeOnboarding4AdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeOnboardingFullAfterPage1AdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeOnboardingFullAfterPage3AdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeSurveyAdLive = MutableLiveData<ApNativeAd?>()
    val nativeConfirmUninstallAdLive = MutableLiveData<ApNativeAd?>()
    val nativeWelcomeAdLive = MutableLiveData<ApNativeAd?>()
    val nativeWelcomeAdStateLive = MutableLiveData(NativeAdLoadState.IDLE)

    // Auto-resolve config for each loaded native ad
    private val adConfigMap = mutableMapOf<ApNativeAd, AdUnitConfig>()
    fun getAdConfig(ad: ApNativeAd): AdUnitConfig? = adConfigMap[ad]

    private var interSplashAd: ApInterstitialAd? = null
    private var interOnboarding: ApInterstitialAd? = null
    private var interWelcomeAd: ApInterstitialAd? = null
    private fun loadNativeInternal(
        activity: Activity,
        config: AdUnitConfig,
        layoutRes: Int,
        liveData: MutableLiveData<ApNativeAd?>,
        shouldDisplay: Boolean = true,
        stateLiveData: MutableLiveData<NativeAdLoadState>? = null,
    ) {
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(activity)
            || !activity.isNetworkAvailable()
            || !shouldDisplay
        ) {
            liveData.postValue(null)
            stateLiveData?.postValue(NativeAdLoadState.FAILED)
            return
        }
        stateLiveData?.postValue(NativeAdLoadState.LOADING)
        ANRAdSdk.getInstance()
            .loadNativeAdResultCallback(activity, config.id, layoutRes, object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    adConfigMap[nativeAd] = config
                    liveData.postValue(nativeAd)
                    stateLiveData?.postValue(NativeAdLoadState.LOADED)
                }

                override fun onAdFailedToLoad(adError: LoadAdError?) {
                    super.onAdFailedToLoad(adError)
                    liveData.postValue(null)
                    stateLiveData?.postValue(NativeAdLoadState.FAILED)
                }
            })
    }

    fun loadNativeSurvey(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity, AdRemoteConfig.native_survey, layoutRes, nativeSurveyAdLive,
            ANRAdSdk.getInstance()
                .shouldDisplayWidgetUninstall
        )
    }

    fun loadNativeConfirmUninstall(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_confirm_uninstall,
            layoutRes,
            nativeConfirmUninstallAdLive,
            ANRAdSdk.getInstance()
                .shouldDisplayWidgetUninstall
        )
    }

    fun loadNativeLanguage(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_language_1 else AdRemoteConfig.native_language_2
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeLanguageAdLive,
            stateLiveData = nativeLanguageAdStateLive
        )
    }

    fun loadNativeLanguageClick(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_language_1_click else AdRemoteConfig.native_language_2_click
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeLanguageClickAdLive,
            stateLiveData = nativeLanguageClickAdStateLive
        )
    }

    fun loadNativeOnboarding(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_1 else AdRemoteConfig.native_onboarding_2_1
        loadNativeInternal(
            activity, config, layoutRes, nativeOnboarding1AdLive,
            ANRAdSdk.getInstance()
                .shouldDisplayNativeOnboardingNormal1,
            nativeOnboarding1AdStateLive
        )
    }

    fun loadNativeOnboarding4(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_4 else AdRemoteConfig.native_onboarding_2_4
        loadNativeInternal(
            activity, config, layoutRes, nativeOnboarding4AdLive,
            ANRAdSdk.getInstance()
                .shouldDisplayNativeOnboardingNormal2,
            nativeOnboarding4AdStateLive
        )
    }

    fun loadNativeOnboardingFullAfterPage1(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_fullscreen_1_1 else AdRemoteConfig.native_onboarding_fullscreen_2_1
        loadNativeInternal(
            activity, config, layoutRes, nativeOnboardingFullAfterPage1AdLive,
            ANRAdSdk.getInstance().shouldDisplayNativeOnboardingFull1,
            nativeOnboardingFullAfterPage1AdStateLive
        )
    }

    fun loadNativeOnboardingFullAfterPage3(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_fullscreen_1_3 else AdRemoteConfig.native_onboarding_fullscreen_2_3
        loadNativeInternal(
            activity, config, layoutRes, nativeOnboardingFullAfterPage3AdLive,
            ANRAdSdk.getInstance().shouldDisplayNativeOnboardingFull2,
            nativeOnboardingFullAfterPage3AdStateLive
        )
    }

    fun loadNativeWelcome(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity, AdRemoteConfig.native_welcome, layoutRes, nativeWelcomeAdLive,
            ANRAdSdk.getInstance()
                .shouldDisplayNativeWelcomeBack,
            nativeWelcomeAdStateLive
        )
    }

    // ── Dashboard / Test helpers (ignore shouldDisplay) ──

    /** Dedicated LiveData for customization preview – won't collide with real flows */
    val nativeDashboardPreviewLive = MutableLiveData<ApNativeAd?>()

    /**
     * Load a native ad for dashboard preview purposes.
     * Bypasses all shouldDisplay checks so it always loads.
     */
    fun loadNativeForDashboard(activity: Activity, configKey: String, layoutRes: Int) {
        val config = try {
            AdRemoteConfig.getInstance().ads[configKey]
                ?: AdUnitConfig(id = "", isEnable = false)
        } catch (_: Exception) {
            AdUnitConfig(id = "", isEnable = false)
        }
        // Force shouldDisplay = true to bypass SDK limits
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeDashboardPreviewLive,
            shouldDisplay = true
        )
    }

    /** Load native language ad for dashboard – ignores shouldDisplay */
    fun loadNativeLanguageForDashboard(activity: Activity, layoutRes: Int) {
        loadNativeForDashboard(activity, "native_language_1", layoutRes)
    }

    /** Load native onboarding full for dashboard – ignores shouldDisplay */
    fun loadNativeFullForDashboard(activity: Activity, layoutRes: Int) {
        loadNativeForDashboard(activity, "native_onboarding_fullscreen_1_3", layoutRes)
    }

    fun loadInterOnboarding(context: Context, ignoreLimit: Boolean = false) {
        val config = AdRemoteConfig.inter_onboarding
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || (!ignoreLimit && !ANRAdSdk.getInstance()
                .shouldDisplayInterOnboarding)
        ) {
            interOnboarding = null
            return
        }
        interOnboarding =
            ANRAdSdk.getInstance()
                .getInterstitialAds(context, config.id, object : AdCallback() {})
    }

    fun showInterOnboarding(context: Context, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interOnboarding
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context) && (ignoreLimit)
        ) {
            ANRAdSdk.getInstance()
                .forceShowInterstitial(context, interstitial, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        onAction()
                    }
                }, true)
        } else {
            onAction()
        }
    }

    fun loadInterWelcome(context: Context, ignoreLimit: Boolean = false) {
        val config = AdRemoteConfig.inter_welcome
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || (!ignoreLimit && !ANRAdSdk.getInstance()
                .shouldDisplayInterWelcomeBack)
        ) {
            interWelcomeAd = null
            return
        }
        interWelcomeAd =
            ANRAdSdk.getInstance()
                .getInterstitialAds(context, config.id, object : AdCallback() {})
    }

    fun showInterWelcome(context: Context, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interWelcomeAd
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context) && (ignoreLimit ||
                    ANRAdSdk.getInstance()
                        .shouldDisplayInterWelcomeBack)
        ) {
            ANRAdSdk.getInstance()
                .forceShowInterstitial(context, interstitial, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    onAction()
                }
            }, false)
        } else {
            onAction()
        }
    }

    fun loadBanner(
        activity: AppCompatActivity,
        adUnitConfig: AdUnitConfig,
        frAds: FrameLayout,
        isCollapse: Boolean,
    ) {
        if (adUnitConfig.isEnable) {
            removeBannerView(activity, frAds)
            if (isCollapse) ANRAdSdk.getInstance().loadCollapsibleBanner(
                activity,
                adUnitConfig.id,
                AppConstant.CollapsibleGravity.BOTTOM,
                object : AdCallback() {
                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        frAds.goneView()
                        Timber.tag("AdsManager_Banner")
                            .d("Load banner on ${activity.javaClass.simpleName} failed by : ${i?.message}")
                    }
                })
            else ANRAdSdk.getInstance()
                .loadBanner(activity, adUnitConfig.id, object : AdCallback() {
                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        frAds.goneView()
                        Timber.tag("AdsManager_Banner")
                            .d("Load banner on ${activity.javaClass.simpleName} failed by : ${i?.message}")
                    }
                })
        } else {
            frAds.removeAllViews()
            frAds.goneView()
        }
    }

    @SuppressLint("InflateParams")
    private fun removeBannerView(activity: Activity, frAds: FrameLayout) {
        try {
            val container =
                frAds.findViewById<FrameLayout>(com.anrstudio.ads.R.id.banner_container)
            if (container != null) {
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    if (view is AdView) {
                        view.destroy()
                        container.removeView(view)
                    }
                }
            }
            val shimmerFrameLayout = LayoutInflater.from(activity)
                .inflate(com.anrstudio.ads.R.layout.layout_banner_control, null)
            frAds.removeAllViews()
            frAds.addView(shimmerFrameLayout)
        } catch (_: Exception) {
        }
    }

    fun clearAll() {
        nativeLanguageAdLive.postValue(null)
        nativeLanguageClickAdLive.postValue(null)
        nativeLanguageAdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeLanguageClickAdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeOnboarding1AdLive.postValue(null)
        nativeOnboarding4AdLive.postValue(null)
        nativeOnboardingFullAfterPage1AdLive.postValue(null)
        nativeOnboardingFullAfterPage3AdLive.postValue(null)
        nativeOnboarding1AdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeOnboarding4AdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeOnboardingFullAfterPage1AdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeOnboardingFullAfterPage3AdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeSurveyAdLive.postValue(null)
        nativeConfirmUninstallAdLive.postValue(null)
        nativeWelcomeAdLive.postValue(null)
        nativeWelcomeAdStateLive.postValue(NativeAdLoadState.IDLE)
        interSplashAd = null
        interWelcomeAd = null
    }

    private fun Context.isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }
}

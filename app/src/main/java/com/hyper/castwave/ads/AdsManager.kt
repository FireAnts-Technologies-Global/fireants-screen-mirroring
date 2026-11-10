package com.hyper.castwave.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.anrstudio.ads.R
import com.anrstudio.ads.ads.ANRAdSdk
import com.anrstudio.ads.ads.wrapper.ApInterstitialAd
import com.anrstudio.ads.ads.wrapper.ApNativeAd
import com.anrstudio.ads.billing.AppPurchase
import com.anrstudio.ads.dialog.PrepareLoadingAdsDialog
import com.anrstudio.ads.funtion.AdCallback
import com.anrstudio.ads.funtion.AdType
import com.anrstudio.ads.funtion.RewardCallback
import com.anrstudio.ads.util.AppConstant
import com.anrstudio.ads.util.SharePreferenceUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.hyper.castwave.ui.bases.ext.goneView
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
    val nativeOnboarding4AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboardingFullAfterPage1AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboardingFullAfterPage3AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding4AdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeOnboardingFullAfterPage1AdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeOnboardingFullAfterPage3AdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeWelcomeAdLive = MutableLiveData<ApNativeAd?>()
    val nativeWelcomeAdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeHomeAdLive = MutableLiveData<ApNativeAd?>()
    val nativeHomeAdStateLive = MutableLiveData(NativeAdLoadState.IDLE)
    val nativeHowToUseAdLive = MutableLiveData<ApNativeAd?>()
    val nativeHowToUseAdStateLive = MutableLiveData(NativeAdLoadState.IDLE)

    private val adConfigMap = mutableMapOf<ApNativeAd, AdUnitConfig>()
    fun getAdConfig(ad: ApNativeAd): AdUnitConfig? = adConfigMap[ad]

    private var interHomeAd: ApInterstitialAd? = null
    private var interWelcomeBackAd: ApInterstitialAd? = null
    private var interBackAd: ApInterstitialAd? = null
    private var interOnboarding4Ad: ApInterstitialAd? = null
    private var interImportSuccessfulChannelsAd: ApInterstitialAd? = null

    private fun loadNativeInternal(
        activity: Activity,
        config: AdUnitConfig,
        layoutRes: Int,
        liveData: MutableLiveData<ApNativeAd?>,
        shouldDisplay: Boolean = true,
        stateLiveData: MutableLiveData<NativeAdLoadState>? = null,
    ) {
        val isPurchased = AppPurchase.getInstance().isPurchased(activity)
        val isNetworkAvailable = activity.isNetworkAvailable()
        if (!config.isEnable || isPurchased || !isNetworkAvailable || !shouldDisplay) {
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

    fun loadNativeLanguage(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_language_1_1,
            layoutRes,
            nativeLanguageAdLive,
            stateLiveData = nativeLanguageAdStateLive
        )
    }

    fun loadNativeLanguageClick(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_language_1_2,
            layoutRes,
            nativeLanguageClickAdLive,
            stateLiveData = nativeLanguageClickAdStateLive
        )
    }

    fun loadNativeOnboarding4(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity, AdRemoteConfig.native_onboarding_4, layoutRes, nativeOnboarding4AdLive,
            ANRAdSdk.getInstance().shouldDisplayNativeOnboardingNormal2,
            nativeOnboarding4AdStateLive
        )
    }

    fun loadNativeOnboardingFullAfterPage1(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_onboarding_fullscreen_1_1,
            layoutRes,
            nativeOnboardingFullAfterPage1AdLive,
            ANRAdSdk.getInstance().shouldDisplayNativeOnboardingFull1,
            nativeOnboardingFullAfterPage1AdStateLive
        )
    }

    fun loadNativeOnboardingFullAfterPage3(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_onboarding_fullscreen_1_2,
            layoutRes,
            nativeOnboardingFullAfterPage3AdLive,
            ANRAdSdk.getInstance().shouldDisplayNativeOnboardingFull2,
            nativeOnboardingFullAfterPage3AdStateLive
        )
    }

    fun loadNativeWelcome(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity, AdRemoteConfig.native_welcome_back, layoutRes, nativeWelcomeAdLive,
            ANRAdSdk.getInstance().shouldDisplayNativeWelcomeBack,
            nativeWelcomeAdStateLive
        )
    }

    fun loadNativeHome(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_home,
            layoutRes,
            nativeHomeAdLive,
            ANRAdSdk.getInstance().shouldDisplayNativeHome,
            nativeHomeAdStateLive
        )
    }

    fun loadNativeHowToUse(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_how_to_use,
            layoutRes,
            nativeHowToUseAdLive,
            stateLiveData = nativeHowToUseAdStateLive
        )
    }

    private fun loadAndShowInterstitialInternal(
        context: Context,
        config: AdUnitConfig,
        getCached: () -> ApInterstitialAd?,
        setCached: (ApInterstitialAd?) -> Unit,
        onAction: () -> Unit,
    ) {
        if (System.currentTimeMillis() - SharePreferenceUtils.getLastImpressionInterstitialTime(
                context
            )
            < ANRAdSdk.getInstance().adConfig.intervalInterstitialAd * 1000L
        ) {
            onAction()
            return
        }
        if (!config.isEnable || AppPurchase.getInstance().isPurchased(context)) {
            onAction()
            return
        }

        val dialog = PrepareLoadingAdsDialog(context)
        dialog.show()

        var isActionCalled = false
        fun safeAction() {
            if (isActionCalled) return
            isActionCalled = true
            dialog.dismiss()
            onAction()
        }

        val showCallback = object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                safeAction()
            }

            override fun onAdFailedToShow(adError: AdError?) {
                super.onAdFailedToShow(adError)
                safeAction()
            }
        }

        val cached = getCached()
        if (cached?.interstitialAd != null) {
            setCached(null)
            dialog.dismiss()
            ANRAdSdk.getInstance().forceShowInterstitial(context, cached, showCallback, true)
            return
        }

        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable { safeAction() }
        handler.postDelayed(timeoutRunnable, 15_000)

        ANRAdSdk.getInstance()
            .getInterstitialAds(context, config.id, object : AdCallback() {
                override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                    super.onApInterstitialLoad(apInterstitialAd)
                    handler.removeCallbacks(timeoutRunnable)
                    dialog.dismiss()
                    if (apInterstitialAd != null && apInterstitialAd.isReady) {
                        setCached(null)
                        ANRAdSdk.getInstance()
                            .forceShowInterstitial(context, apInterstitialAd, showCallback, true)
                    } else {
                        setCached(null)
                        safeAction()
                    }
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    setCached(null)
                    handler.removeCallbacks(timeoutRunnable)
                    safeAction()
                }
            })
    }

    fun loadAndShowInterHome(context: Context, onAction: () -> Unit) {
        loadAndShowInterstitialInternal(
            context, AdRemoteConfig.inter_home,
            { interHomeAd }, { interHomeAd = it }, onAction,
        )
    }

    fun loadAndShowInterBack(context: Context, onAction: () -> Unit) {
        loadAndShowInterstitialInternal(
            context, AdRemoteConfig.inter_back,
            { interBackAd }, { interBackAd = it }, onAction,
        )
    }

    fun loadAndShowInterWelcome(context: Context, onAction: () -> Unit) {
        if (!ANRAdSdk.getInstance().shouldDisplayInterWelcomeBack) {
            onAction()
            return
        }
        loadAndShowInterstitialInternal(
            context, AdRemoteConfig.inter_welcome_back,
            { interWelcomeBackAd }, { interWelcomeBackAd = it }, onAction,
        )
    }

    fun loadAndShowInterOnboarding4(context: Context, onAction: () -> Unit) {
        if (!ANRAdSdk.getInstance().shouldDisplayInterOnboarding) {
            onAction()
            return
        }
        loadAndShowInterstitialInternal(
            context, AdRemoteConfig.inter_onboarding_4,
            { interOnboarding4Ad }, { interOnboarding4Ad = it }, onAction,
        )
    }

    fun loadAndShowInterImportSuccessfulChannels(context: Context, onAction: () -> Unit) {
        loadAndShowInterstitialInternal(
            context, AdRemoteConfig.inter_import_successful_channels,
            { interImportSuccessfulChannelsAd }, { interImportSuccessfulChannelsAd = it }, onAction,
        )
    }

    fun loadAndShowRewardScreenMirroring(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onFail: () -> Unit,
    ) {
        val config = AdRemoteConfig.rewarded_screen_mirroring
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(activity)
            || !activity.isNetworkAvailable()
        ) {
            onFail()
            return
        }

        val dialog = PrepareLoadingAdsDialog(activity)
        dialog.show()

        var isCompleted = false
        fun safeFail() {
            if (isCompleted) return
            isCompleted = true
            dialog.dismiss()
            onFail()
        }

        val handler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable { safeFail() }
        handler.postDelayed(timeoutRunnable, 15_000)

        var earned = false
        val rewardCallback = object : RewardCallback {
            override fun onUserEarnedReward(item: RewardItem?) {
                earned = true
            }

            override fun onRewardedAdClosed() {
                if (isCompleted) return
                isCompleted = true
                dialog.dismiss()
                if (earned) onUserEarnedReward() else onFail()
            }

            override fun onRewardedAdFailedToShow(codeError: Int) {
                safeFail()
            }

            override fun onAdClicked() {}
            override fun onAdClicked(
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?
            ) {
            }

            override fun onAdImpression() {}
            override fun onAdLogRev(
                adValue: AdValue?,
                adUnitId: String?,
                mediationAdapterClassName: String?,
                adType: AdType?,
            ) {
            }
        }

        ANRAdSdk.getInstance()
            .initRewardAds(activity, config.id, object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                    super.onRewardAdLoaded(rewardedAd)
                    handler.removeCallbacks(timeoutRunnable)
                    dialog.dismiss()
                    if (rewardedAd == null) {
                        safeFail()
                        return
                    }
                    ANRAdSdk.getInstance().showRewardAds(activity, rewardedAd, rewardCallback)
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    handler.removeCallbacks(timeoutRunnable)
                    safeFail()
                }
            })
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

    fun loadBannerCollapsible(
        activity: AppCompatActivity,
        adUnitConfig: AdUnitConfig,
        frAds: FrameLayout,
    ) = loadBanner(activity, adUnitConfig, frAds, isCollapse = true)

    @SuppressLint("InflateParams")
    private fun removeBannerView(activity: Activity, frAds: FrameLayout) {
        try {
            val container = frAds.findViewById<FrameLayout>(R.id.banner_container)
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
                .inflate(R.layout.layout_banner_control, null)
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
        nativeOnboarding4AdLive.postValue(null)
        nativeOnboardingFullAfterPage1AdLive.postValue(null)
        nativeOnboardingFullAfterPage3AdLive.postValue(null)
        nativeOnboarding4AdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeOnboardingFullAfterPage1AdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeOnboardingFullAfterPage3AdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeWelcomeAdLive.postValue(null)
        nativeWelcomeAdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeHomeAdLive.postValue(null)
        nativeHomeAdStateLive.postValue(NativeAdLoadState.IDLE)
        nativeHowToUseAdLive.postValue(null)
        nativeHowToUseAdStateLive.postValue(NativeAdLoadState.IDLE)
        interHomeAd = null
        interWelcomeBackAd = null
        interBackAd = null
        interOnboarding4Ad = null
        interImportSuccessfulChannelsAd = null
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

package com.hyper.castwave.ui.component.onboarding

import androidx.activity.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.anrstudio.ads.ads.ANRAdSdk
import com.anrstudio.ads.billing.AppPurchase
import com.hyper.castwave.R
import com.hyper.castwave.ads.AdRemoteConfig
import com.hyper.castwave.ads.AdsManager
import com.hyper.castwave.ads.RemoteConfigUtils
import com.hyper.castwave.ads.native_onboarding_fullscreen_1_1
import com.hyper.castwave.ads.native_onboarding_fullscreen_1_2
import com.hyper.castwave.ui.bases.BaseActivity
import com.hyper.castwave.ui.bases.NavigationBarConfig
import com.hyper.castwave.ui.bases.ext.isNetwork
import com.hyper.castwave.ui.component.onboarding.adapter.OnboardingAdapter
import com.hyper.castwave.ui.component.onboarding.model.NativeFullPlacement
import com.hyper.castwave.ui.component.onboarding.model.OnboardingItem
import com.hyper.castwave.ui.component.onboarding.viewmodel.OnboardingViewModel
import com.hyper.castwave.utils.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class OnBoardingActivity : BaseActivity<ActivityOnboardingBinding>() {

    override fun getLayoutActivity(): Int = R.layout.activity_onboarding


    override val navigationBarConfig: NavigationBarConfig
        get() = NavigationBarConfig(
            isVisible = RemoteConfigUtils.getOnShowNavigationButton(),
            applyPadding = RemoteConfigUtils.getOnShowNavigationButton()
        )


    private val onboardingViewModel by viewModels<OnboardingViewModel>()

    private lateinit var onboardingAdapter: OnboardingAdapter

    private val onboardingItems = mutableListOf<OnboardingItem>()

    override fun initViews() {
        initPage()
        initOnboardingItems()
        mBinding.root.postDelayed({
            if (AppPurchase.getInstance().isPurchased(this)) {
                return@postDelayed
            }


            if (ANRAdSdk.getInstance()
                    .shouldDisplayNativeOnboardingFull1
            ) {
                AdsManager.loadNativeOnboardingFullAfterPage1(
                    this,
                    R.layout.layout_native_onboarding_full
                )
            }


            if (ANRAdSdk.getInstance()
                    .shouldDisplayNativeOnboardingFull2
            ) {
                AdsManager.loadNativeOnboardingFullAfterPage3(
                    this,
                    R.layout.layout_native_onboarding_full
                )
            }
        }, 100L)
    }


    override fun observeData() {
        super.observeData()

        onboardingViewModel.isNeedNextPage.observe(this) { isNeed ->
            if (isNeed == true) {
                val currentPosition = mBinding.viewPager.currentItem
                if (currentPosition < onboardingAdapter.itemCount - 1) {
                    mBinding.viewPager.currentItem = currentPosition + 1
                } else {
                    startNextActivity()
                }
                onboardingViewModel.onNextPageHandled()
            }
        }

        AdsManager.nativeOnboardingFullAfterPage1AdStateLive.observe(this) { state ->
            if (state == AdsManager.NativeAdLoadState.FAILED) {
                removeNativeFullPage(NativeFullPlacement.AFTER_PAGE_1)
            }
        }
        AdsManager.nativeOnboardingFullAfterPage3AdStateLive.observe(this) { state ->
            if (state == AdsManager.NativeAdLoadState.FAILED) {
                removeNativeFullPage(NativeFullPlacement.AFTER_PAGE_2)
            }
        }

    }

    private fun initPage() {
        onboardingAdapter = OnboardingAdapter(supportFragmentManager, lifecycle)
        mBinding.viewPager.adapter = onboardingAdapter
        mBinding.viewPager.clipToPadding = false
        mBinding.viewPager.clipChildren = false
        mBinding.viewPager.offscreenPageLimit = 1
        mBinding.viewPager.getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer(100))
        compositePageTransformer.addTransformer { view, position ->
            val r = 1 - abs(position)
            view.scaleY = 0.8f + r * 0.2f
            val absPosition = abs(position)
            view.alpha = 1.0f - (1.0f - 0.3f) * absPosition
        }
        mBinding.viewPager.setPageTransformer(compositePageTransformer)

        mBinding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
            }
        })
    }


    private fun initOnboardingItems() {
        onboardingItems.clear()
        onboardingItems.add(
            OnboardingItem(
                title = R.string.onboarding_title_1,
                description = R.string.onboarding_des_1,
                textButton = R.string.next,
                imageResId = R.drawable.ic_vietnamese,
                positionIndicator = 0,
                isHasNativeOnPage1 = true
            )
        )

        addNativeFullPageIfEligible(NativeFullPlacement.AFTER_PAGE_1)

        onboardingItems.add(
            OnboardingItem(
                title = R.string.onboarding_title_2,
                description = R.string.onboarding_des_2,
                textButton = R.string.next,
                imageResId = R.drawable.ic_vietnamese,
                positionIndicator = 1
            )
        )

        addNativeFullPageIfEligible(NativeFullPlacement.AFTER_PAGE_2)

        onboardingItems.add(
            OnboardingItem(
                title = R.string.onboarding_title_3,
                description = R.string.onboarding_des_3,
                textButton = R.string.next,
                imageResId = R.drawable.ic_vietnamese,
                positionIndicator = 2,

                )
        )

        onboardingItems.add(
            OnboardingItem(
                title = R.string.onboarding_title_4,
                description = R.string.onboarding_des_4,
                textButton = R.string.next,
                imageResId = R.drawable.ic_vietnamese,
                positionIndicator = 3,
                isHasNativeOnPage4 = true
            )
        )
        onboardingAdapter.submitData(onboardingItems)
    }

    private fun addNativeFullPageIfEligible(placement: NativeFullPlacement) {
        val config = when (placement) {
            NativeFullPlacement.AFTER_PAGE_1 -> AdRemoteConfig.native_onboarding_fullscreen_1_1


            NativeFullPlacement.AFTER_PAGE_2 -> AdRemoteConfig.native_onboarding_fullscreen_1_2

        }
        val shouldDisplay = when (placement) {
            NativeFullPlacement.AFTER_PAGE_1 -> ANRAdSdk.getInstance()
                .shouldDisplayNativeOnboardingFull1

            NativeFullPlacement.AFTER_PAGE_2 -> ANRAdSdk.getInstance()
                .shouldDisplayNativeOnboardingFull2
        }
        val isPurchased = AppPurchase.getInstance().isPurchased(this)
        if (isNetwork(this) && config.isEnable && shouldDisplay && !isPurchased) {
            onboardingItems.add(OnboardingItem(nativeFullPlacement = placement))
        }
    }

    private fun removeNativeFullPage(placement: NativeFullPlacement) {
        val index = onboardingItems.indexOfFirst { it.nativeFullPlacement == placement }
        if (index != -1) {
            onboardingItems.removeAt(index)
            onboardingAdapter.submitData(onboardingItems)
        }
    }

    private fun startNextActivity() {
        AdsManager.loadAndShowInterOnboarding4(this) {
            appSharedPref.firstOnBoarding = false
            Routes.startMainActivity(this)
            finish()
        }
    }

    override fun onBackPressed() {
    }
}

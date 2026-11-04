package com.anrstudio.template.ui.component.onboarding.adapter

import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.anrstudio.ads.ads.wrapper.ApNativeAd
import com.bumptech.glide.Glide
import com.anrstudio.template.R
import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.populateNativeAdView
import com.anrstudio.template.databinding.FragmentOnboardingPageBinding
import com.anrstudio.template.ui.bases.BaseFragment
import com.anrstudio.template.ui.bases.ext.click
import com.anrstudio.template.ui.bases.ext.goneView
import com.anrstudio.template.ui.bases.ext.invisibleView
import com.anrstudio.template.ui.bases.ext.parcelable
import com.anrstudio.template.ui.bases.ext.visibleView
import com.anrstudio.template.ui.component.onboarding.model.NativeFullPlacement
import com.anrstudio.template.ui.component.onboarding.model.OnboardingItem
import com.anrstudio.template.ui.component.onboarding.viewmodel.OnboardingViewModel

class OnboardingPageFragment : BaseFragment<FragmentOnboardingPageBinding>() {

    override fun getLayoutFragment(): Int = R.layout.fragment_onboarding_page

    companion object {
        private const val ARG_ONBOARDING_ITEM = "arg_onboarding_item"

        fun newInstance(onboardingItem: OnboardingItem) = OnboardingPageFragment().apply {
            arguments = bundleOf(ARG_ONBOARDING_ITEM to onboardingItem)
        }
    }

    private val onboardingViewModel by activityViewModels<OnboardingViewModel>()

    private var onboardingItem: OnboardingItem = OnboardingItem(
        title = R.string.onboarding_title_1,
        description = R.string.onboarding_des_1,
        textButton = R.string.next,
        imageResId = R.drawable.ic_vietnamese,
        positionIndicator = 0
    )

    override fun initViews() {
        arguments?.parcelable<OnboardingItem>(ARG_ONBOARDING_ITEM)?.let {
            onboardingItem = it
        }
        updateLayout()
    }

    override fun observerData() {
        observeAdChannel()
    }

    override fun onClickViews() {
        mBinding.btnNext.click { onboardingViewModel.onNextClicked() }
        mBinding.btnNext1.click { onboardingViewModel.onNextClicked() }
        mBinding.imgCloseAdsFull.click { onboardingViewModel.onNextClicked() }
    }

    private fun observeAdChannel() {
        val channels: Pair<MutableLiveData<ApNativeAd?>, MutableLiveData<AdsManager.NativeAdLoadState>> =
            when {
                onboardingItem.isHasNativeOnPage4 ->
                    AdsManager.nativeOnboarding4AdLive to AdsManager.nativeOnboarding4AdStateLive

                onboardingItem.nativeFullPlacement == NativeFullPlacement.AFTER_PAGE_1 ->
                    AdsManager.nativeOnboardingFullAfterPage1AdLive to
                            AdsManager.nativeOnboardingFullAfterPage1AdStateLive

                onboardingItem.nativeFullPlacement == NativeFullPlacement.AFTER_PAGE_2 ->
                    AdsManager.nativeOnboardingFullAfterPage3AdLive to
                            AdsManager.nativeOnboardingFullAfterPage3AdStateLive

                else -> {
                    renderNoAd()
                    return
                }
            }
        channels.second.observe(viewLifecycleOwner) { state -> renderAdState(state) }
        channels.first.observe(viewLifecycleOwner) { ad ->
            if (ad != null) renderLoadedAd(ad)
        }
    }

    private fun renderAdState(state: AdsManager.NativeAdLoadState) {
        when (state) {
            AdsManager.NativeAdLoadState.IDLE,
            AdsManager.NativeAdLoadState.LOADING -> renderAdLoading()

            AdsManager.NativeAdLoadState.LOADED -> Unit
            AdsManager.NativeAdLoadState.FAILED -> renderAd(null)
        }
    }

    private fun renderLoadedAd(ad: ApNativeAd) {
        renderAdLoading()
        renderAd(ad)
    }

    private fun renderAdLoading() {
        if (onboardingItem.nativeFullPlacement != null) {
            mBinding.layoutAdsFull.visibleView()
            mBinding.layoutAdFullContent.goneView()
            mBinding.shimmerAdsFull.shimmerNativeFull.visibleView()
            mBinding.shimmerAdsFull.shimmerNativeFull.startShimmer()
            mBinding.imgCloseAdsFull.invisibleView()
            mBinding.layoutContent.invisibleView()
        } else {
            mBinding.layoutContent.visibleView()
            mBinding.layoutAds.visibleView()
            mBinding.layoutAdContent.goneView()
            mBinding.shimmerAds.shimmerNativeMedium.visibleView()
            mBinding.shimmerAds.shimmerNativeMedium.startShimmer()
            mBinding.layoutAdsFull.invisibleView()
            mBinding.imgCloseAdsFull.invisibleView()
        }
    }

    private fun renderAd(ad: ApNativeAd?) {
        if (onboardingItem.nativeFullPlacement != null) {
            if (ad != null) {
                mBinding.layoutAdsFull.visibleView()
                mBinding.layoutAdFullContent.visibleView()
                mBinding.imgCloseAdsFull.visibleView()
                mBinding.layoutContent.invisibleView()
                populateNativeAdView(
                    requireActivity(),
                    ad,
                    mBinding.layoutAdFullContent,
                    mBinding.shimmerAdsFull.shimmerNativeFull
                )
            } else {
                mBinding.shimmerAdsFull.shimmerNativeFull.stopShimmer()
                mBinding.layoutContent.visibleView()
                mBinding.layoutAdsFull.invisibleView()
                mBinding.layoutAds.invisibleView()
                mBinding.imgCloseAdsFull.invisibleView()
            }
        } else if (onboardingItem.isHasNativeOnPage1 || onboardingItem.isHasNativeOnPage4) {
            if (ad != null) {
                mBinding.layoutAds.visibleView()
                mBinding.layoutAdContent.visibleView()
                populateNativeAdView(
                    requireActivity(),
                    ad,
                    mBinding.layoutAdContent,
                    mBinding.shimmerAds.shimmerNativeMedium
                )
            } else {
                mBinding.shimmerAds.shimmerNativeMedium.stopShimmer()
                mBinding.layoutAds.invisibleView()
            }
            mBinding.layoutContent.visibleView()
            mBinding.layoutAdsFull.invisibleView()
            mBinding.imgCloseAdsFull.invisibleView()
        }
    }

    private fun renderNoAd() {
        mBinding.layoutContent.visibleView()
        mBinding.layoutAds.goneView()
        mBinding.layoutAdsFull.invisibleView()
        mBinding.imgCloseAdsFull.invisibleView()
    }

    private fun updateLayout() {
        Glide.with(this).load(onboardingItem.imageResId).into(mBinding.imgOnboarding)
        mBinding.tvTitle.text = getString(onboardingItem.title)
        mBinding.tvDes.text = getString(onboardingItem.description)

        if (onboardingItem.positionIndicator == 3) {
            mBinding.onAction1.goneView()
            mBinding.onAction2.visibleView()

            mBinding.imgIndicator0.setImageResource(R.drawable.ic_onboarding_indicator)
            mBinding.imgIndicator1.setImageResource(R.drawable.ic_onboarding_indicator)
            mBinding.imgIndicator2.setImageResource(R.drawable.ic_onboarding_indicator)
            mBinding.imgIndicator3.setImageResource(R.drawable.ic_onboarding_indicator)
            when (onboardingItem.positionIndicator) {
                0 -> mBinding.imgIndicator0.setImageResource(R.drawable.ic_onboarding_indicator_selected)
                1 -> mBinding.imgIndicator1.setImageResource(R.drawable.ic_onboarding_indicator_selected)
                2 -> mBinding.imgIndicator2.setImageResource(R.drawable.ic_onboarding_indicator_selected)
                3 -> mBinding.imgIndicator3.setImageResource(R.drawable.ic_onboarding_indicator_selected)
            }
        } else {
            mBinding.onAction1.visibleView()
            mBinding.onAction2.goneView()
            mBinding.btnNext1.text = getString(onboardingItem.textButton)

            mBinding.imgIndicator01.setImageResource(R.drawable.ic_onboarding_indicator)
            mBinding.imgIndicator11.setImageResource(R.drawable.ic_onboarding_indicator)
            mBinding.imgIndicator21.setImageResource(R.drawable.ic_onboarding_indicator)
            mBinding.imgIndicator31.setImageResource(R.drawable.ic_onboarding_indicator)
            when (onboardingItem.positionIndicator) {
                0 -> mBinding.imgIndicator01.setImageResource(R.drawable.ic_onboarding_indicator_selected)
                1 -> mBinding.imgIndicator11.setImageResource(R.drawable.ic_onboarding_indicator_selected)
                2 -> mBinding.imgIndicator21.setImageResource(R.drawable.ic_onboarding_indicator_selected)
                3 -> mBinding.imgIndicator31.setImageResource(R.drawable.ic_onboarding_indicator_selected)
            }
        }
    }
}

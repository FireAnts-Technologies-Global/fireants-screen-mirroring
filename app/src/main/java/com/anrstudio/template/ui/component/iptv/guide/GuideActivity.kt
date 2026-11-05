package com.anrstudio.template.ui.component.iptv.guide

import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.AdRemoteConfig
import com.anrstudio.template.ads.inter_back
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.anrstudio.template.R
import com.anrstudio.template.data.iptv.model.TVChannelSourceType
import com.anrstudio.template.databinding.CastActivityGuideBinding
import com.anrstudio.template.ui.component.iptv.IptvAppConstants
import com.anrstudio.template.ui.component.iptv.IptvBaseActivity
import com.anrstudio.template.ui.component.iptv.addchannel.AddChannelActivity.Companion.EXTRA_SOURCE_TYPE
import com.anrstudio.template.ui.component.iptv.guide.file.GuideFileFragment
import com.anrstudio.template.ui.component.iptv.guide.stream.GuideStreamFragment
import com.anrstudio.template.ui.component.iptv.guide.url.GuideUrlFragment
import com.anrstudio.template.ui.component.iptv.home.TabInfo
import com.anrstudio.template.ui.component.iptv.home.TabLayoutHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GuideActivity : IptvBaseActivity<CastActivityGuideBinding>() {

    override fun getLayoutActivity(): Int = R.layout.cast_activity_guide

    private lateinit var pagerAdapter: GuidePagerAdapter
    private lateinit var tabLayoutHelper: TabLayoutHelper
    private var initialTabPosition = 0



    override fun initViews() {
        super.initViews()
        isHasShowAddWhenBack = intent.getBooleanExtra(IptvAppConstants.EXTRA_SHOW_ADS_WHEN_BACK, false)
        getSourceTypeFromIntent()
        setupTopAppBar()
        setupViewPager()
        setupTabLayout()
        setInitialTab()
    }

    private fun setInitialTab() {
        mBinding.viewPager.post {
            mBinding.viewPager.setCurrentItem(initialTabPosition, false)
        }
    }

    private fun getSourceTypeFromIntent() {
        val sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE) ?: TVChannelSourceType.URL.value
        initialTabPosition = when (sourceType) {
            TVChannelSourceType.URL.value -> 0
            TVChannelSourceType.FILE.value -> 1
            TVChannelSourceType.STREAM.value -> 2
            else -> 0
        }
    }

    private fun setupTopAppBar() {
        mBinding.icBack.setOnClickListener {
            AdsManager.loadAndShowInterstitial(this@GuideActivity, AdRemoteConfig.inter_back) {
                finish()
            }
        }
    }

    private fun setupViewPager() {
        pagerAdapter = GuidePagerAdapter(this)
        mBinding.viewPager.adapter = pagerAdapter
        mBinding.viewPager.isUserInputEnabled = false
    }

    private fun setupTabLayout() {
        val tabInfos = listOf(
            TabInfo(getString(R.string.cast_import_url), R.drawable.cast_ic_tab_url),
            TabInfo(getString(R.string.cast_import_stream_link), R.drawable.cast_ic_tab_stream),
            TabInfo(getString(R.string.cast_upload_file), R.drawable.cast_ic_tab_file),
        )
        tabLayoutHelper = TabLayoutHelper(
            activity = this,
            tabLayout = mBinding.tabLayout,
            viewPager = mBinding.viewPager,
            tabInfos = tabInfos,
        )
        tabLayoutHelper.setup()
    }

    override fun onDestroy() {
        super.onDestroy()
        tabLayoutHelper.cleanup()
    }

    private class GuidePagerAdapter(
        activity: GuideActivity,
    ) : FragmentStateAdapter(activity.supportFragmentManager, activity.lifecycle) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> GuideUrlFragment()
            1 -> GuideFileFragment()
            2 -> GuideStreamFragment()
            else -> GuideUrlFragment()
        }
    }
}

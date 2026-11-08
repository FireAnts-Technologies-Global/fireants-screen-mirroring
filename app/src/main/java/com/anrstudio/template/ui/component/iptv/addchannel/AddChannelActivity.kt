package com.anrstudio.template.ui.component.iptv.addchannel

import com.anrstudio.template.ads.AdsManager
import com.anrstudio.template.ads.AdRemoteConfig
import com.anrstudio.template.ads.inter_back
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastActivityAddChannelBinding
import com.anrstudio.template.data.iptv.model.TVChannelSourceType
import com.anrstudio.template.ui.component.iptv.IptvBaseActivity
import com.anrstudio.template.ui.component.iptv.addchannel.file.AddChannelFileFragment
import com.anrstudio.template.ui.component.iptv.addchannel.url.AddChannelUrlFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.max

@AndroidEntryPoint
class AddChannelActivity : IptvBaseActivity<CastActivityAddChannelBinding>() {

    companion object {
        const val EXTRA_SOURCE_TYPE = "extra_source_type"
    }

    override var isHasShowAddWhenBack = true



    private lateinit var pagerAdapter: AddChannelPagerAdapter
    private var onPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var initialTabPosition = 0

    override fun getLayoutActivity(): Int = R.layout.cast_activity_add_channel

    override fun initViews() {
        super.initViews()

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.setPadding(0, 0, 0, max(imeHeight, navHeight))
            insets
        }

        initialTabPosition = resolveInitialTabPosition()
        setupTopAppBar()
        setupViewPager()
        setupTabSwitcher()
    }

    private fun resolveInitialTabPosition(): Int {
        val sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE) ?: TVChannelSourceType.URL.value
        return when (sourceType) {
            TVChannelSourceType.FILE.value -> 1
            else -> 0
        }
    }

    private fun setupTopAppBar() {
        mBinding.icBack.setOnClickListener {
            AdsManager.loadAndShowInterstitial(this@AddChannelActivity, AdRemoteConfig.inter_back) {
                finish()
            }
        }
    }

    private fun setupViewPager() {
        pagerAdapter = AddChannelPagerAdapter(this)
        mBinding.viewPager.isUserInputEnabled = false
        mBinding.viewPager.adapter = pagerAdapter

        onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabSelection(position)
            }
        }
        mBinding.viewPager.registerOnPageChangeCallback(onPageChangeCallback!!)
        mBinding.viewPager.setCurrentItem(initialTabPosition, false)
        updateTabSelection(initialTabPosition)
    }

    private fun setupTabSwitcher() {
        mBinding.btnTabUrl.setOnClickListener {
            mBinding.viewPager.setCurrentItem(0, true)
        }
        mBinding.btnTabFile.setOnClickListener {
            mBinding.viewPager.setCurrentItem(1, true)
        }
    }

    private fun updateTabSelection(position: Int) {
        val isUrlTab = position == 0
        val activeColor = ContextCompat.getColor(this, R.color.cast_white)
        val inactiveColor = ContextCompat.getColor(this, R.color.cast_color_tab_inactive)
        val activeFont = ResourcesCompat.getFont(this, R.font.cast_poppins_semi_bold)
        val inactiveFont = ResourcesCompat.getFont(this, R.font.cast_poppins_medium)

        mBinding.btnTabUrl.setTextColor(if (isUrlTab) activeColor else inactiveColor)
        mBinding.btnTabFile.setTextColor(if (isUrlTab) inactiveColor else activeColor)
        mBinding.btnTabUrl.typeface = if (isUrlTab) activeFont else inactiveFont
        mBinding.btnTabFile.typeface = if (isUrlTab) inactiveFont else activeFont

        mBinding.tabSwitcher.post {
            val halfWidth = mBinding.tabSwitcher.width / 2f
            mBinding.viewTabIndicator.animate()
                .translationX(if (isUrlTab) 0f else halfWidth)
                .setDuration(200L)
                .start()
        }
    }

    override fun onDestroy() {
        onPageChangeCallback?.let { mBinding.viewPager.unregisterOnPageChangeCallback(it) }
        onPageChangeCallback = null
        super.onDestroy()
    }

    private class AddChannelPagerAdapter(
        activity: AddChannelActivity,
    ) : FragmentStateAdapter(activity.supportFragmentManager, activity.lifecycle) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AddChannelUrlFragment()
                1 -> AddChannelFileFragment()
                else -> AddChannelUrlFragment()
            }
        }
    }
}

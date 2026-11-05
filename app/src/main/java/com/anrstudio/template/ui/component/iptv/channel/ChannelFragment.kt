package com.anrstudio.template.ui.component.iptv.channel

import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastFragmentChannelBinding
import com.anrstudio.template.data.iptv.model.TVChannelSourceType
import com.anrstudio.template.ui.bases.BaseFragment
import com.anrstudio.template.ui.component.iptv.IptvRoutes
import com.anrstudio.template.ui.component.iptv.channel.all.ChannelAllFragment
import com.anrstudio.template.ui.component.iptv.channel.category.ChannelCategoryFragment

class ChannelFragment : BaseFragment<CastFragmentChannelBinding>() {
    private lateinit var pagerAdapter: ChannelPagerAdapter
    private var onPageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_channel

    override fun initViews() {
        super.initViews()
        setupViewPager()
        setupTabSwitcher()
        setupClickListeners()
    }

    private fun setupViewPager() {
        pagerAdapter = ChannelPagerAdapter(this)
        mBinding.viewPager.adapter = pagerAdapter
        mBinding.viewPager.isUserInputEnabled = true

        onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabSelection(position)
            }
        }
        mBinding.viewPager.registerOnPageChangeCallback(onPageChangeCallback!!)
        updateTabSelection(0)
    }

    private fun setupTabSwitcher() {
        mBinding.btnTabChannels.setOnClickListener {
            mBinding.viewPager.setCurrentItem(0, true)
        }
        mBinding.btnTabPlaylist.setOnClickListener {
            mBinding.viewPager.setCurrentItem(1, true)
        }
    }

    private fun updateTabSelection(position: Int) {
        val isChannelsTab = position == 0
        val activeColor = ContextCompat.getColor(requireContext(), R.color.cast_white)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.cast_color_tab_inactive)
        val activeFont = ResourcesCompat.getFont(requireContext(), R.font.cast_poppins_semi_bold)
        val inactiveFont = ResourcesCompat.getFont(requireContext(), R.font.cast_poppins_medium)

        mBinding.btnTabChannels.setTextColor(if (isChannelsTab) activeColor else inactiveColor)
        mBinding.btnTabPlaylist.setTextColor(if (isChannelsTab) inactiveColor else activeColor)
        mBinding.btnTabChannels.typeface = if (isChannelsTab) activeFont else inactiveFont
        mBinding.btnTabPlaylist.typeface = if (isChannelsTab) inactiveFont else activeFont

        mBinding.tabSwitcher.post {
            val halfWidth = mBinding.tabSwitcher.width / 2f
            mBinding.viewTabIndicator.animate()
                .translationX(if (isChannelsTab) 0f else halfWidth)
                .setDuration(200L)
                .start()
        }
    }

    private fun setupClickListeners() {
        mBinding.fabAddChannel.setOnClickListener {
            IptvRoutes.startAddChannelActivity(
                requireActivity(),
                TVChannelSourceType.URL.value
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        onPageChangeCallback?.let {
            mBinding.viewPager.unregisterOnPageChangeCallback(it)
        }
        onPageChangeCallback = null
    }

    private class ChannelPagerAdapter(
        fragment: Fragment,
    ) : FragmentStateAdapter(fragment.childFragmentManager, fragment.lifecycle) {

        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ChannelAllFragment()
                1 -> ChannelCategoryFragment()
                else -> ChannelAllFragment()
            }
        }
    }
}

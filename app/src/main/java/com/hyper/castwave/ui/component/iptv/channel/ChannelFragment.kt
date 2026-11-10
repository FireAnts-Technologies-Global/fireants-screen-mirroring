package com.hyper.castwave.ui.component.iptv.channel

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.core.content.res.ResourcesCompat
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastFragmentChannelBinding
import com.hyper.castwave.ui.bases.BaseFragment
import com.hyper.castwave.ui.component.iptv.channel.all.ChannelAllFragment
import com.hyper.castwave.ui.component.iptv.channel.category.ChannelCategoryFragment

class ChannelFragment : BaseFragment<CastFragmentChannelBinding>() {
    private lateinit var pagerAdapter: ChannelPagerAdapter
    private var onPageChangeCallback: ViewPager2.OnPageChangeCallback? = null

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_channel

    override fun initViews() {
        super.initViews()
        setupViewPager()
        setupTabSwitcher()
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
        val activeColor = requireContext().getColor(R.color.cast_white)
        val inactiveColor = requireContext().getColor(R.color.cast_home_text_secondary)
        val activeFont = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
        val inactiveFont = ResourcesCompat.getFont(requireContext(), R.font.inter_semibold)

        mBinding.btnTabChannels.setTextColor(if (isChannelsTab) activeColor else inactiveColor)
        mBinding.btnTabPlaylist.setTextColor(if (isChannelsTab) inactiveColor else activeColor)
        mBinding.btnTabChannels.typeface = if (isChannelsTab) activeFont else inactiveFont
        mBinding.btnTabPlaylist.typeface = if (isChannelsTab) inactiveFont else activeFont

        mBinding.tabSwitcher.layoutParams = mBinding.tabSwitcher.layoutParams.apply {
            height = requireContext().resources.getDimensionPixelSize(
                if (isChannelsTab) R.dimen.cast_channels_tab_height
                else R.dimen.cast_playlists_tab_height
            )
        }
        mBinding.tabSwitcher.post {
            val selectedOffset = mBinding.tabSwitcher.width / 2f - 2f
            mBinding.viewTabIndicator.animate()
                .translationX(if (isChannelsTab) 0f else selectedOffset)
                .setDuration(200L)
                .start()
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

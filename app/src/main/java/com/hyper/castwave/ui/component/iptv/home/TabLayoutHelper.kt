package com.hyper.castwave.ui.component.iptv.home

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.hyper.castwave.R

class TabLayoutHelper(
    private val activity: FragmentActivity,
    private val tabLayout: TabLayout,
    private val viewPager: ViewPager2,
    private val tabInfos: List<TabInfo>
) {
    private var mediator: TabLayoutMediator? = null

    fun setup() {
        mediator = TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val tabInfo = tabInfos[position]
            tab.customView = createTabView(tabInfo.title, tabInfo.iconRes)
        }.also { it.attach() }

        tabLayout.getTabAt(0)?.let { updateTabView(it, true) }

        tabLayout.addOnTabSelectedListener(createTabSelectedListener())
    }

    private fun createTabView(title: String, iconRes: Int): View {
        val view = LayoutInflater.from(activity).inflate(R.layout.cast_item_tab, null)
        val icon = view.findViewById<ImageView>(R.id.iv_tab_icon)
        val text = view.findViewById<TextView>(R.id.tv_tab_text)

        icon.setImageResource(iconRes)
        text.text = title

        return view
    }

    private fun createTabSelectedListener() = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
            updateTabView(tab, true)
        }   

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            updateTabView(tab, false)
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {}
    }

    private fun updateTabView(tab: TabLayout.Tab?, isSelected: Boolean) {
        tab?.customView?.let { view ->
            val container = view.findViewById<View>(R.id.tab_container)
            val icon = view.findViewById<ImageView>(R.id.iv_tab_icon)
            val text = view.findViewById<TextView>(R.id.tv_tab_text)

            container.isSelected = isSelected

            val textColor = if (isSelected) {
                ContextCompat.getColor(activity, R.color.cast_black)
            } else {
                ContextCompat.getColor(activity, R.color.cast_nav_item_unselected_text)
            }

            icon.setColorFilter(textColor)
            text.setTextColor(textColor)
        }
    }

    fun cleanup() {
        mediator?.detach()
        mediator = null
    }
}

data class TabInfo(
    val title: String,
    val iconRes: Int
)



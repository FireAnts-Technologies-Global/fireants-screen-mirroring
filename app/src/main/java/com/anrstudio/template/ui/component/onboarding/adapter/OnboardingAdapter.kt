package com.anrstudio.template.ui.component.onboarding.adapter

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.anrstudio.template.ui.component.onboarding.model.NativeFullPlacement
import com.anrstudio.template.ui.component.onboarding.model.OnboardingItem

class OnboardingAdapter(
    manager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(manager, lifecycle) {

    private val fragmentRefs = mutableMapOf<Int, Fragment>()
    private val items = mutableListOf<OnboardingItem>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitData(items: List<OnboardingItem>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        val item = items[position]
        return if (item.nativeFullPlacement != null) {
            if (item.nativeFullPlacement == NativeFullPlacement.AFTER_PAGE_1) -1L else -2L
        } else {
            item.positionIndicator.toLong()
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        return items.any { item ->
            val id = if (item.nativeFullPlacement != null) {
                if (item.nativeFullPlacement == NativeFullPlacement.AFTER_PAGE_1) -1L else -2L
            } else {
                item.positionIndicator.toLong()
            }
            id == itemId
        }
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = OnboardingPageFragment.newInstance(items[position])
        fragmentRefs[position] = fragment
        return fragment
    }

}
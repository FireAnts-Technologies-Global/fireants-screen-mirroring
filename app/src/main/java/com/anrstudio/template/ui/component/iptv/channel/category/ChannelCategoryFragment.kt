package com.anrstudio.template.ui.component.iptv.channel.category

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastFragmentChannelCategoryBinding
import com.anrstudio.template.data.iptv.model.TVChannelSourceType
import com.anrstudio.template.ui.bases.BaseFragment
import com.anrstudio.template.ui.component.iptv.channel.adapter.CategoryAdapter
import com.anrstudio.template.ui.component.iptv.IptvAppConstants
import com.anrstudio.template.data.iptv.model.TVChannel
import com.anrstudio.template.ui.component.iptv.viewmodel.IptvHomeViewModel
import com.anrstudio.template.ui.component.iptv.IptvRoutes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChannelCategoryFragment : BaseFragment<CastFragmentChannelCategoryBinding>() {
    private val viewModel: IptvHomeViewModel by viewModels({ requireActivity() })
    private lateinit var categoryAdapter: CategoryAdapter
    private var searchQuery: String = ""
    private var allCategories: List<CategoryAdapter.CategoryItem> = emptyList()

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_channel_category

    override fun initViews() {
        super.initViews()
        setupRecyclerView()
        setupEmptyState()
        observeCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            onItemClick = { position ->
                val categoryItem = categoryAdapter.currentList[position]
                navigateToAllChannels(categoryItem)
            }
        )

        mBinding.rvChannels.layoutManager = LinearLayoutManager(requireContext())
        mBinding.rvChannels.adapter = categoryAdapter
    }

    private fun setupEmptyState() {
        mBinding.layoutEmpty.btnAddChannel.setOnClickListener {
            IptvRoutes.startAddChannelActivity(requireActivity(), TVChannelSourceType.URL.value)
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            viewModel.allChannels.collect { channels ->
                allCategories = loadCategories(channels)
                applySearchFilter()
            }
        }
    }
    
    private fun applySearchFilter() {
        val filteredCategories = if (searchQuery.isNotEmpty()) {
            allCategories.filter { 
                it.name.contains(searchQuery, ignoreCase = true)
            }
        } else {
            allCategories
        }
        showEmptyState(filteredCategories.isEmpty(), searchQuery.isNotEmpty())
        categoryAdapter.submitList(filteredCategories)
    }
    
    fun updateSearchQuery(query: String) {
        val newQuery = query.trim()
        if (newQuery != searchQuery) {
            searchQuery = newQuery
            applySearchFilter()
        }
    }

    private suspend fun loadCategories(channels: List<TVChannel>): List<CategoryAdapter.CategoryItem> {
        val allGroups = viewModel.getAllChannelGroups()
        val channelsByCategory = channels.groupBy { channel ->
            if (channel.groupId.isNotEmpty()) {
                val group = allGroups.find { it.name == channel.groupId }
                group?.displayName?.takeIf { it.isNotEmpty() }
                    ?: group?.name?.takeIf { it.isNotEmpty() }
                    ?: channel.groupTitle.takeIf { it.isNotEmpty() }
                    ?: channel.groupId
            } else {
                channel.groupTitle.takeIf { it.isNotEmpty() } ?: "Uncategorized"
            }
        }
        val categoryItems = mutableListOf<CategoryAdapter.CategoryItem>()
        var categoryId = 1L
        
        channelsByCategory.forEach { (categoryName, channelList) ->
            if (categoryName.isNotEmpty() && channelList.isNotEmpty() && categoryName != "Uncategorized") {
                val firstChannel = channelList.first()
                val categoryGroupId = firstChannel.groupId.ifEmpty {
                    val group = allGroups.find {
                        it.displayName == categoryName || it.name == categoryName
                    }
                    group?.name ?: ""
                }
                
                categoryItems.add(
                    CategoryAdapter.CategoryItem(
                        id = categoryId++,
                        name = categoryName,
                        channelCount = channelList.size,
                        groupId = categoryGroupId
                    )
                )
            }
        }

        return categoryItems.sortedBy { it.name }
    }

    private fun showEmptyState(isShow: Boolean, isSearch: Boolean = false) {
        mBinding.rvChannels.visibility = if (isShow) View.GONE else View.VISIBLE
        if (isShow) {
            if (isSearch) {
                mBinding.layoutEmpty.root.visibility = View.GONE
                mBinding.layoutEmptySearch.root.visibility = View.VISIBLE
            } else {
                mBinding.layoutEmpty.root.visibility = View.VISIBLE
                mBinding.layoutEmptySearch.root.visibility = View.GONE
            }
        } else {
            mBinding.layoutEmpty.root.visibility = View.GONE
            mBinding.layoutEmptySearch.root.visibility = View.GONE
        }
    }

    private fun navigateToAllChannels(categoryItem: CategoryAdapter.CategoryItem) {
        IptvRoutes.startAllChannelActivity(
            requireActivity(),
            categoryItem.name,
            categoryItem.groupId,
            IptvAppConstants.GROUP_TYPE_CATEGORY
        )
    }

    override fun onClickViews() {
        super.onClickViews()
    }
}



package com.anrstudio.template.ui.component.iptv.channel.category

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastFragmentChannelCategoryBinding
import com.anrstudio.template.ui.bases.BaseFragment
import com.anrstudio.template.ui.component.iptv.channel.adapter.CategoryAdapter
import com.anrstudio.template.ui.component.iptv.IptvAppConstants
import com.anrstudio.template.ui.component.iptv.viewmodel.IptvHomeViewModel
import com.anrstudio.template.ui.component.iptv.IptvRoutes
import com.anrstudio.template.data.iptv.model.TVChannelSourceType
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
            },
            onAddClick = {
                IptvRoutes.startAddChannelActivity(requireActivity(), TVChannelSourceType.URL.value)
            },
        )

        mBinding.rvChannels.layoutManager = LinearLayoutManager(requireContext())
        mBinding.rvChannels.adapter = categoryAdapter
    }

    private fun setupEmptyState() {
        mBinding.layoutEmpty.btnAddChannel.setOnClickListener {
            IptvRoutes.startAddChannelActivity(
                requireActivity(),
                TVChannelSourceType.URL.value
            )
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            viewModel.allSourcesByImport.collect { sources ->
                allCategories = sources.map { item ->
                    CategoryAdapter.CategoryItem(
                        id = item.group.sourceId,
                        name = item.group.displayName.ifEmpty { item.group.name },
                        channelCount = item.channelCount,
                        groupId = item.group.sourceId.toString(),
                    )
                }
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
        val listWithAddRow = if (filteredCategories.isNotEmpty()) {
            filteredCategories + CategoryAdapter.CategoryItem(
                id = Long.MIN_VALUE,
                name = "",
                channelCount = 0,
                isAddPlaylist = true,
            )
        } else {
            filteredCategories
        }
        categoryAdapter.submitList(listWithAddRow)
    }
    
    fun updateSearchQuery(query: String) {
        val newQuery = query.trim()
        if (newQuery != searchQuery) {
            searchQuery = newQuery
            applySearchFilter()
        }
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
            IptvAppConstants.GROUP_TYPE_SOURCE
        )
    }

    override fun onClickViews() {
        super.onClickViews()
    }
}



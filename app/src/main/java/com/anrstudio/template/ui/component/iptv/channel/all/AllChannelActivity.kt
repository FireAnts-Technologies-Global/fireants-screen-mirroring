package com.anrstudio.template.ui.component.iptv.channel.all

import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastActivityAllChannelBinding
import com.anrstudio.template.data.iptv.model.TVChannel
import com.anrstudio.template.ui.component.iptv.IptvBaseActivity
import com.anrstudio.template.ui.component.iptv.dialog.DeleteChannelDialog
import com.anrstudio.template.ui.component.iptv.dialog.EditChannelDialog
import com.anrstudio.template.data.iptv.model.TVChannelSourceType
import com.anrstudio.template.ui.component.iptv.IptvAppConstants
import com.anrstudio.template.ui.bases.ext.showToastById
import com.anrstudio.template.ui.component.iptv.channel.all.adapter.ChannelAdapter
import com.anrstudio.template.ui.component.iptv.channel.all.dialog.EditChannelPopup
import com.anrstudio.template.ui.component.iptv.viewmodel.IptvHomeViewModel
import com.anrstudio.template.ui.component.iptv.IptvRoutes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllChannelActivity : IptvBaseActivity<CastActivityAllChannelBinding>() {

    override var isHasShowAddWhenBack = true

    private val viewModel: IptvHomeViewModel by viewModels()
    private lateinit var channelAdapter: ChannelAdapter
    private var currentPopup: EditChannelPopup? = null
    private var currentEditDialog: EditChannelDialog? = null
    private var currentDeleteDialog: DeleteChannelDialog? = null
    private var categoryName: String = ""
    private var groupId: String = ""
    private var filterType: String = ""
    private var groupType: String = GROUP_TYPE_CATEGORY
    private var allChannels: List<TVChannel> = emptyList()
    private var filteredChannels: List<TVChannel> = emptyList()

    override fun getLayoutActivity(): Int = R.layout.cast_activity_all_channel




    override fun initViews() {
        super.initViews()
        loadAds()
        getIntentData()
        setupHeader()
        setupRecyclerView()
        setupSearchBar()
        setupEmptyState()
        observeChannels()
    }

    private fun loadAds() {
    }

    private fun getIntentData() {
        categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""
        groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: ""
        filterType = intent.getStringExtra(EXTRA_FILTER_TYPE) ?: ""
        groupType = intent.getStringExtra(EXTRA_GROUP_TYPE) ?: GROUP_TYPE_CATEGORY
    }

    private fun setupHeader() {

        mBinding.icBack.setOnClickListener {
            onActivityBackPressed()
        }

        updateChannelCount(0)
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(
            onChannelClick = { channel ->
                IptvRoutes.startPlaylistActivity(this, channel, groupType, filterType)
            },
            onFavoriteClick = { channel, isFavorite ->
                updateFavoriteStatus(channel, isFavorite)
            },
            onMoreClick = { channel, anchorView ->
                showEditChannelPopup(channel, anchorView)
            }
        )

        mBinding.rvChannels.layoutManager = GridLayoutManager(this, 1)
        mBinding.rvChannels.adapter = channelAdapter
    }

    private fun setupSearchBar() {
        mBinding.etSearch.setOnEditorActionListener { _, _, _ ->
            false
        }
        mBinding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChannels(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun setupEmptyState() {
        mBinding.layoutEmpty.btnAddChannel.setOnClickListener {
            IptvRoutes.startAddChannelActivity(this, TVChannelSourceType.URL.value)
        }
    }

    private fun observeChannels() {
        lifecycleScope.launch {
            when (filterType) {
                IptvAppConstants.FILTER_TYPE_FAVORITE -> {
                    viewModel.favoriteChannels.collect { channels ->
                        filteredChannels = channels
                        updateTitle(getString(R.string.cast_favorite))
                        updateChannelCount(channels.size)
                        applySearchAndDisplay()
                    }
                }

                IptvAppConstants.FILTER_TYPE_RECENT -> {
                    viewModel.recentChannels.collect { channels ->
                        filteredChannels = channels
                        updateTitle(getString(R.string.cast_recent))
                        updateChannelCount(channels.size)
                        applySearchAndDisplay()
                    }
                }

                else -> {
                    when (groupType) {
                        GROUP_TYPE_SOURCE -> {
                            if (groupId.isNotEmpty()) {
                                val sourceId = groupId.toLongOrNull() ?: 0L
                                val source = viewModel.getChannelSourceById(sourceId)
                                val sourceName = source?.name
                                    ?: categoryName.ifEmpty { getString(R.string.cast_group) }
                                updateTitle(sourceName)

                                viewModel.getTVChannelsBySource(sourceId).collect { channels ->
                                    filteredChannels = channels
                                    updateChannelCount(channels.size)
                                    applySearchAndDisplay()
                                }
                            } else {
                                filteredChannels = emptyList()
                                updateTitle(getString(R.string.cast_group))
                                updateChannelCount(0)
                                applySearchAndDisplay()
                            }
                        }

                        GROUP_TYPE_CATEGORY -> {
                            viewModel.allChannels.collect { channels ->
                                allChannels = channels
                                loadChannelsByCategory()
                            }
                        }

                        else -> {
                            viewModel.allChannels.collect { channels ->
                                allChannels = channels
                                loadChannelsByCategory()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun applySearchAndDisplay() {
        val searchQuery = mBinding.etSearch.text?.toString() ?: ""
        if (searchQuery.isNotEmpty()) {
            filterChannels(searchQuery)
        } else {
            channelAdapter.submitList(filteredChannels)
            showEmptyState(filteredChannels.isEmpty(), false)
        }
    }

    private fun loadChannelsByCategory() {
        lifecycleScope.launch {
            filteredChannels = when (groupType) {
                GROUP_TYPE_CATEGORY -> {
                    if (groupId.isNotEmpty()) {
                        allChannels.filter { it.groupId == groupId }
                    } else {
                        val allGroups = viewModel.getAllChannelGroups()
                        val targetGroup = allGroups.find {
                            it.displayName == categoryName || it.name == categoryName
                        }
                        if (targetGroup != null) {
                            allChannels.filter { it.groupId == targetGroup.name }
                        } else {
                            allChannels.filter { it.groupTitle == categoryName }
                        }
                    }
                }

                else -> {
                    if (groupId.isNotEmpty()) {
                        allChannels.filter { it.groupId == groupId }
                    } else {
                        allChannels.filter { it.groupTitle == categoryName }
                    }
                }
            }

            val categoryTitle = when {
                categoryName.isNotEmpty() -> categoryName
                filteredChannels.isNotEmpty() -> {
                    val firstChannel = filteredChannels.first()
                    firstChannel.groupTitle.ifEmpty { getString(R.string.cast_category) }
                }

                else -> getString(R.string.cast_category)
            }
            updateTitle(categoryTitle)
            updateChannelCount(filteredChannels.size)

            applySearchAndDisplay()
        }
    }

    private fun updateTitle(title: String) {
        mBinding.tvTitle.text = title
    }

    private fun filterChannels(query: String) {
        val filtered = if (query.isEmpty()) {
            filteredChannels
        } else {
            filteredChannels.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.groupTitle.contains(query, ignoreCase = true)
            }
        }
        updateChannelCount(filtered.size)
        channelAdapter.submitList(filtered)
        showEmptyState(filtered.isEmpty(), query.isNotEmpty())
    }

    private fun updateChannelCount(count: Int) {
        val countText = if (count == 1) {
            getString(R.string.cast_channel_count_singular, count)
        } else {
            getString(R.string.cast_channels_count, count)
        }
        mBinding.tvChannelCount.text = countText
    }

    private fun showEmptyState(show: Boolean, isSearch: Boolean = false) {
        mBinding.rvChannels.visibility = if (show) View.GONE else View.VISIBLE
        if (show) {
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

    private fun updateFavoriteStatus(channel: TVChannel, isFavorite: Boolean) {
        lifecycleScope.launch {
            viewModel.updateChannelFavorite(channel, isFavorite)
            if (isFavorite) {
                showToastById(R.string.cast_txt_add_favorite_success)
            } else {
                showToastById(R.string.cast_txt_delete_favorite_success)
            }
        }
    }

    private fun showEditChannelPopup(channel: TVChannel, anchorView: View) {
        currentPopup?.dismiss()

        currentPopup = EditChannelPopup(
            context = this,
            channel = channel,
            onEditClick = { channel ->
                showEditChannelDialog(channel)
            },
            onDeleteClick = { channel ->
                showDeleteChannelDialog(channel)
            }
        )
        currentPopup?.show(anchorView)
    }

    private fun showEditChannelDialog(channel: TVChannel) {
        currentEditDialog?.dismiss()

        val currentName = channel.name
        currentEditDialog = EditChannelDialog(
            context = this,
            currentName = currentName,
            onSaveClick = { newName ->
                if (newName.isNotEmpty() && newName != currentName) {
                    updateChannelName(channel.copy(name = newName))
                }
            }
        )
        currentEditDialog?.show()
    }

    private fun showDeleteChannelDialog(channel: TVChannel) {
        currentDeleteDialog?.dismiss()

        currentDeleteDialog = DeleteChannelDialog(
            context = this,
            onDeleteClick = {
                deleteChannel(channel)
            }
        )
        currentDeleteDialog?.show()
    }

    private fun updateChannelName(channel: TVChannel) {
        lifecycleScope.launch {
            viewModel.updateChannel(channel)
        }
    }

    private fun deleteChannel(channel: TVChannel) {
        lifecycleScope.launch {
            viewModel.deleteChannel(channel)
            when {
                filterType == IptvAppConstants.FILTER_TYPE_FAVORITE ||
                        filterType == IptvAppConstants.FILTER_TYPE_RECENT -> {
                }

                groupType == GROUP_TYPE_SOURCE && groupId.isNotEmpty() -> {

                }

                else -> {
                    loadChannelsByCategory()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        currentPopup?.dismiss()
        currentEditDialog?.dismiss()
        currentDeleteDialog?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        currentPopup?.dismiss()
        currentPopup = null
        currentEditDialog?.dismiss()
        currentEditDialog = null
        currentDeleteDialog?.dismiss()
        currentDeleteDialog = null
    }

    companion object {
        const val EXTRA_CATEGORY_NAME = "extra_category_name"
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_FILTER_TYPE = "extra_filter_type"
        const val EXTRA_GROUP_TYPE = "extra_group_type"
        const val GROUP_TYPE_CATEGORY = IptvAppConstants.GROUP_TYPE_CATEGORY
        const val GROUP_TYPE_SOURCE = IptvAppConstants.GROUP_TYPE_SOURCE
    }
}


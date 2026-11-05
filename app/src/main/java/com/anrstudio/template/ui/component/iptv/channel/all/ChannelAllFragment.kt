package com.anrstudio.template.ui.component.iptv.channel.all

import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastFragmentChannelAllBinding
import com.anrstudio.template.data.iptv.model.TVChannel
import com.anrstudio.template.data.iptv.model.TVChannelSourceType
import com.anrstudio.template.ui.bases.BaseFragment
import com.anrstudio.template.ui.component.iptv.dialog.DeleteChannelDialog
import com.anrstudio.template.ui.component.iptv.dialog.EditChannelDialog
import com.anrstudio.template.ui.component.iptv.IptvAppConstants
import com.anrstudio.template.ui.bases.ext.showToastById
import com.anrstudio.template.ui.component.iptv.channel.all.adapter.ChannelAdapter
import com.anrstudio.template.ui.component.iptv.channel.all.dialog.EditChannelPopup
import com.anrstudio.template.ui.component.iptv.viewmodel.IptvHomeViewModel
import com.anrstudio.template.ui.component.iptv.IptvRoutes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChannelAllFragment : BaseFragment<CastFragmentChannelAllBinding>() {
    private val viewModel: IptvHomeViewModel by viewModels({ requireActivity() })
    private lateinit var channelAdapter: ChannelAdapter
    private var currentPopup: EditChannelPopup? = null
    private var currentEditDialog: EditChannelDialog? = null
    private var currentDeleteDialog: DeleteChannelDialog? = null
    private var searchQuery: String = ""
    private var observeJob: Job? = null

    override fun getLayoutFragment(): Int = R.layout.cast_fragment_channel_all

    override fun initViews() {
        super.initViews()
        setupRecyclerView()
        setupEmptyState()
        observeData()
    }

    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter(
            onChannelClick = { channel ->
                IptvRoutes.startPlaylistActivity(
                    fromActivity = requireActivity(),
                    tvChanel = channel,
                    filterType = IptvAppConstants.FILTER_ALL,
                    isShowAdsWhenBack = true
                )
            },
            onFavoriteClick = { channel, isFavorite ->
                updateFavoriteStatus(channel, isFavorite)
            },
            onMoreClick = { channel, anchorView ->
                showEditChannelPopup(channel, anchorView)
            }
        )

        mBinding.rvChannels.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = channelAdapter
        }
    }

    private fun setupEmptyState() {
        mBinding.layoutEmpty.btnAddChannel.setOnClickListener {
            IptvRoutes.startAddChannelActivity(requireActivity(), TVChannelSourceType.URL.value)
        }
    }

    private fun observeData() {
        observeJob?.cancel()
        observeJob = lifecycleScope.launch {
            val channelsFlow = if (searchQuery.isNotEmpty()) {
                viewModel.searchTVChannels(searchQuery)
            } else {
                viewModel.allChannels
            }
            channelsFlow.collect { channels ->
                channelAdapter.submitList(channels.sortedBy { it.addedTime })
                showEmptyState(channels.isEmpty(), searchQuery.isNotEmpty())
            }
        }
    }

    private fun updateFavoriteStatus(channel: TVChannel, isFavorite: Boolean) {
        lifecycleScope.launch {
            viewModel.updateChannelFavorite(channel, isFavorite)
            context?.showToastById(if (isFavorite) R.string.cast_added_to_favorites else R.string.cast_removed_from_favorites)
        }
    }

    fun updateSearchQuery(query: String) {
        val newQuery = query.trim()
        if (newQuery != searchQuery) {
            searchQuery = newQuery
            observeData()
        }
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

    fun updateChannels(channels: List<TVChannel>) {
        channelAdapter.submitList(channels)
        showEmptyState(channels.isEmpty(), searchQuery.isNotEmpty())
    }

    private fun showEditChannelPopup(channel: TVChannel, anchorView: View) {
        currentPopup?.dismiss()

        currentPopup = EditChannelPopup(
            context = requireContext(),
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
            context = requireContext(),
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
            context = requireContext(),
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
        }
    }

    override fun onPause() {
        super.onPause()
        currentPopup?.dismiss()
        currentEditDialog?.dismiss()
        currentDeleteDialog?.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        observeJob?.cancel()
        currentPopup?.dismiss()
        currentPopup = null
        currentEditDialog?.dismiss()
        currentEditDialog = null
        currentDeleteDialog?.dismiss()
        currentDeleteDialog = null
    }

    override fun onClickViews() {
        super.onClickViews()
    }
}


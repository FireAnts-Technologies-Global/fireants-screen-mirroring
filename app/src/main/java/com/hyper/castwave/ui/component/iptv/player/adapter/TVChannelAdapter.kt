package com.hyper.castwave.ui.component.iptv.player.adapter

import android.util.Log
import android.view.View
import androidx.databinding.ViewDataBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastItemTvChannelBinding
import com.hyper.castwave.data.iptv.model.TVChannel
import com.hyper.castwave.ui.bases.BaseRecyclerViewAdapter
import com.hyper.castwave.ui.bases.ext.click

class TVChannelAdapter(private val selectedUrl: String) : BaseRecyclerViewAdapter<TVChannel>() {

    override fun getItemLayout() = R.layout.cast_item_tv_channel

    var onClickTVChannel: ((TVChannel, Int, Boolean) -> Unit)? = null

    var onChangePositionSelected: ((Int, Int, Boolean) -> Unit)? = null

    var urlSelected = selectedUrl

    override fun setData(
        binding: ViewDataBinding,
        item: TVChannel,
        layoutPosition: Int
    ) {
        if (binding is CastItemTvChannelBinding) {
            val isSelected = urlSelected == item.url
            binding.cvContent.isSelected = isSelected
            binding.tvChannelName.text = item.name
            loadThumbnail(binding, item)

            val onChannelClick = {
                if (urlSelected != item.url) onClickTVChannel?.invoke(item, layoutPosition, false)
                Log.d("Kai", "setData: ${item.url}")
            }

            binding.root.click { onChannelClick() }
            binding.btnWatchNow.click { onChannelClick() }
        }
    }

    private fun loadThumbnail(binding: CastItemTvChannelBinding, item: TVChannel) {
        binding.shimmerThumbnail.visibility = View.VISIBLE
        if (!binding.shimmerThumbnail.isShimmerStarted) {
            binding.shimmerThumbnail.startShimmer()
        }

        val thumbnailUrl = item.logo.ifBlank { item.url }
        Glide.with(binding.root.context)
            .load(thumbnailUrl.takeIf { it.isNotBlank() && !it.startsWith("content://") && !it.startsWith("file://") })
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .placeholder(R.drawable.cast_bg_channel_thumb)
            .error(R.drawable.cast_thumbnail_default)
            .into(binding.ivThumbnail)

        binding.shimmerThumbnail.stopShimmer()
        binding.shimmerThumbnail.visibility = View.GONE
    }

    override fun submitData(newData: List<TVChannel>) {
        list.clear()
        list.addAll(newData)
        invokeWhenChangePositionSelected(true)
        notifyDataSetChanged()
    }

    private fun invokeWhenChangePositionSelected(isHasScroll: Boolean) {
        val indexSelected = list.indexOfFirst { it.url == urlSelected }
        onChangePositionSelected?.invoke(indexSelected + 1, list.size, isHasScroll)
    }

    fun updateSelectedItem(tvChannel: TVChannel) {
        val oldIndex = list.indexOfFirst { it.url == urlSelected }
        urlSelected = tvChannel.url
        val newIndex = list.indexOfFirst { it.url == urlSelected }
        if (oldIndex > -1) notifyItemChanged(oldIndex)
        if (newIndex > -1) notifyItemChanged(newIndex)
        invokeWhenChangePositionSelected(false)
    }

    fun onNextChanel() {
        val indexSelected = list.indexOfFirst { it.url == urlSelected }
        if (indexSelected > -1 && list.isNotEmpty()) {
            val chanel = if (indexSelected < list.size - 2) list[indexSelected + 1]
            else list[0]
            if (urlSelected != chanel.url) onClickTVChannel?.invoke(chanel, indexSelected, true)
        }
    }

    fun onPrevChanel() {
        val indexSelected = list.indexOfFirst { it.url == urlSelected }
        if (indexSelected > -1 && list.isNotEmpty()) {
            val chanel = if (indexSelected > 0) list[indexSelected - 1]
            else list.last()
            if (urlSelected != chanel.url) onClickTVChannel?.invoke(chanel, indexSelected, true)
        }
    }
}

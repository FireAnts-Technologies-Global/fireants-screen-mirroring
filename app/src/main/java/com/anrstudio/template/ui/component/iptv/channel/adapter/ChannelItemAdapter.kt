package com.anrstudio.template.ui.component.iptv.channel.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastItemIptvChannelBinding

class ChannelItemAdapter(
    private val onHeartClick: (Int, Boolean) -> Unit,
    private val onMoreClick: (Int) -> Unit,
    private val onItemClick: (Int) -> Unit
) : ListAdapter<ChannelItemAdapter.ChannelItem, ChannelItemAdapter.ChannelViewHolder>(ChannelItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = CastItemIptvChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ChannelViewHolder(
        private val binding: CastItemIptvChannelBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: ChannelItem, position: Int) {
            binding.tvChannelName.text = channel.name

            // Load thumbnail
            if (channel.thumbnailUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(channel.thumbnailUrl)
                    .centerCrop()
                    .into(binding.ivThumbnail)
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.cast_ic_play)
            }

            // Set heart icon
            binding.icHeart.isSelected = channel.isFavorite

            // Click listeners
            binding.root.setOnClickListener {
                onItemClick(position)
            }

            binding.icHeart.setOnClickListener {
                val newFavoriteState = !channel.isFavorite
                onHeartClick(position, newFavoriteState)
            }

            binding.icMore.setOnClickListener {
                onMoreClick(position)
            }
        }
    }

    data class ChannelItem(
        val id: Long,
        val name: String,
        val thumbnailUrl: String,
        val isFavorite: Boolean = false
    )

    private class ChannelItemDiffCallback : DiffUtil.ItemCallback<ChannelItem>() {
        override fun areItemsTheSame(oldItem: ChannelItem, newItem: ChannelItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChannelItem, newItem: ChannelItem): Boolean {
            return oldItem == newItem
        }
    }
}


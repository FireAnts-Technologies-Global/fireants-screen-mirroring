package com.anrstudio.template.ui.component.iptv.channel.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastItemCategoryBinding
import com.anrstudio.template.databinding.CastItemPlaylistAddBinding

class CategoryAdapter(
    private val onItemClick: (Int) -> Unit,
    private val onAddClick: () -> Unit,
) : ListAdapter<CategoryAdapter.CategoryItem, RecyclerView.ViewHolder>(CategoryDiffCallback()) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isAddPlaylist) VIEW_TYPE_ADD else VIEW_TYPE_PLAYLIST

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_ADD) {
            PlaylistAddViewHolder(CastItemPlaylistAddBinding.inflate(inflater, parent, false))
        } else {
            CategoryViewHolder(CastItemCategoryBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> holder.bind(getItem(position), position)
            is PlaylistAddViewHolder -> holder.bind()
        }
    }

    inner class CategoryViewHolder(
        private val binding: CastItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: CategoryItem, position: Int) {
            binding.tvCategoryName.text = category.name
            binding.tvChannelCount.text = binding.root.context.getString(
                R.string.cast_channels_count,
                category.channelCount
            )

            binding.root.setOnClickListener {
                onItemClick(position)
            }

            binding.btnMore.setOnClickListener {
                onItemClick(position)
            }
        }
    }

    inner class PlaylistAddViewHolder(
        private val binding: CastItemPlaylistAddBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.btnAddPlaylist.setOnClickListener { onAddClick() }
        }
    }

    data class CategoryItem(
        val id: Long,
        val name: String,
        val channelCount: Int,
        val groupId: String = "",
        val iconRes: Int = 0,
        val isAddPlaylist: Boolean = false,
    )

    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem == newItem
        }
    }

    private companion object {
        const val VIEW_TYPE_PLAYLIST = 0
        const val VIEW_TYPE_ADD = 1
    }
}


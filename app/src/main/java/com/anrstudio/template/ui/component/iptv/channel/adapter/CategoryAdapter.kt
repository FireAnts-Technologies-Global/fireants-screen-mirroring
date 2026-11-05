package com.anrstudio.template.ui.component.iptv.channel.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastItemCategoryBinding

class CategoryAdapter(
    private val onItemClick: (Int) -> Unit
) : ListAdapter<CategoryAdapter.CategoryItem, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = CastItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
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

    data class CategoryItem(
        val id: Long,
        val name: String,
        val channelCount: Int,
        val groupId: String = "",
        val iconRes: Int = 0
    )

    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem == newItem
        }
    }
}


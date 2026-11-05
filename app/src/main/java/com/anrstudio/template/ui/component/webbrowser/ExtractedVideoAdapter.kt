package com.anrstudio.template.ui.component.webbrowser

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastItemExtractedVideoBinding
import com.anrstudio.template.domain.model.ExtractedVideo

class ExtractedVideoAdapter(
    private val onItemClick: (ExtractedVideo) -> Unit,
) : ListAdapter<ExtractedVideo, ExtractedVideoAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CastItemExtractedVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: CastItemExtractedVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(video: ExtractedVideo) {
            binding.tvVideoName.text = video.name.ifEmpty { video.url }
            val metaParts = mutableListOf<String>()
            if (video.quality.isNotEmpty()) metaParts.add(video.quality)
            if (video.format.isNotEmpty()) metaParts.add(video.format)
            if(metaParts.isNotEmpty()){
                binding.tvVideoMeta.text = metaParts.joinToString(" • ")
                binding.tvVideoMeta.visibility = View.VISIBLE
            }else{
                binding.tvVideoMeta.visibility = View.GONE
            }

            if (video.thumbnail.isNotEmpty()) {
                val thumbnailUrl = if (video.thumbnail.startsWith("http://")) {
                    video.thumbnail.replace("http://", "https://")
                } else {
                    video.thumbnail
                }
                
                Glide.with(binding.imgVideoThumb.context)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.cast_img_thumb)
                    .error(R.drawable.cast_img_thumb)
                    .centerCrop()
                    .into(binding.imgVideoThumb)
            } else {
                Glide.with(binding.imgVideoThumb.context)
                    .load(R.drawable.cast_img_thumb)
                    .into(binding.imgVideoThumb)
            }
            
            binding.root.setOnClickListener { onItemClick(video) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ExtractedVideo>() {
        override fun areItemsTheSame(oldItem: ExtractedVideo, newItem: ExtractedVideo): Boolean =
            oldItem.url == newItem.url

        override fun areContentsTheSame(oldItem: ExtractedVideo, newItem: ExtractedVideo): Boolean =
            oldItem == newItem
    }
}

package com.hyper.castwave.ui.component.media

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastItemMediaBinding
import com.hyper.castwave.domain.model.MediaItem
import com.hyper.castwave.domain.model.MediaType
import androidx.core.net.toUri

class MediaItemAdapter(
    private val onItemClick: (MediaItem) -> Unit,
) : ListAdapter<MediaItem, MediaItemAdapter.ViewHolder>(DiffCallback) {

    var castingItemId: String? = null
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        }

    inner class ViewHolder(private val binding: CastItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem, castingId: String?) {
            val isCasting = item.id == castingId
            binding.viewCastingOverlay.visibility = if (isCasting) View.VISIBLE else View.GONE
            binding.imgCastingIcon.visibility = if (isCasting) View.VISIBLE else View.GONE

            if (item.type == MediaType.VIDEO && !isCasting) {
                binding.imgPlay.visibility = View.VISIBLE
            } else {
                binding.imgPlay.visibility = View.GONE
            }
            updateAspectRatio(item.type)
            when (item.type) {
                MediaType.IMAGE -> bindImage(item)
                MediaType.VIDEO -> bindVideo(item)
                MediaType.AUDIO -> bindAudio(item)
            }
            binding.root.setOnClickListener { onItemClick(item) }
        }

        private fun updateAspectRatio(type: MediaType) {
            val layoutParams = binding.flThumbnail.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.dimensionRatio = when (type) {
                MediaType.IMAGE -> "H,101:112"
                MediaType.VIDEO -> "H,79:90"
                MediaType.AUDIO -> "H,101:112"
            }
            layoutParams.height = binding.root.resources.getDimensionPixelSize(
                if (type == MediaType.VIDEO) R.dimen.cast_media_tile_video_height else R.dimen.cast_media_tile_photo_height
            )
            binding.flThumbnail.layoutParams = layoutParams
        }

        private fun bindImage(item: MediaItem) {
            binding.flThumbnail.setBackgroundColor(0)
            binding.imgThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            Glide.with(binding.imgThumbnail)
                .load(item.uri.toUri())
                .centerCrop()
                .placeholder(R.drawable.cast_bg_audio_item)
                .into(binding.imgThumbnail)
            binding.tvDuration.visibility = View.GONE
            binding.tvName.visibility = View.GONE
        }

        private fun bindVideo(item: MediaItem) {
            binding.flThumbnail.setBackgroundColor(0)
            binding.imgThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
            binding.imgPlay.setColorFilter(android.graphics.Color.WHITE)
            Glide.with(binding.imgThumbnail)
                .load(item.uri.toUri())
                .centerCrop()
                .placeholder(R.drawable.cast_bg_audio_item)
                .into(binding.imgThumbnail)
            val duration = item.duration
            if (duration != null && duration > 0) {
                binding.tvDuration.visibility = View.VISIBLE
                binding.tvDuration.text = duration.formatDuration()
            } else {
                binding.tvDuration.visibility = View.GONE
            }
            binding.tvName.visibility = View.GONE
        }

        private fun bindAudio(item: MediaItem) {
            binding.flThumbnail.setBackgroundResource(R.drawable.cast_bg_audio_item)
            binding.imgThumbnail.scaleType = android.widget.ImageView.ScaleType.CENTER
            binding.imgThumbnail.setImageResource(R.drawable.cast_ic_audio)
            val duration = item.duration
            if (duration != null && duration > 0) {
                binding.tvDuration.visibility = View.VISIBLE
                binding.tvDuration.text = duration.formatDuration()
            } else {
                binding.tvDuration.visibility = View.GONE
            }
            binding.tvName.visibility = View.VISIBLE
            binding.tvName.text = item.name
        }

        private fun Long.formatDuration(): String {
            val totalSeconds = this / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes % 60, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CastItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), castingItemId)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
                oldItem == newItem
        }
    }
}

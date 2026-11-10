package com.hyper.castwave.ui.component.iptv.channel.all.adapter

import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.hyper.castwave.R
import com.hyper.castwave.databinding.CastItemIptvChannelBinding
import com.hyper.castwave.data.iptv.model.TVChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

class ChannelAdapter(
    private val onChannelClick: (TVChannel) -> Unit,
    private val onFavoriteClick: (TVChannel, Boolean) -> Unit,
    private val onMoreClick: (TVChannel, View) -> Unit,
) : ListAdapter<TVChannel, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = CastItemIptvChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelViewHolder(
        private val binding: CastItemIptvChannelBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: TVChannel) {
            binding.ivThumbnail.setImageDrawable(null)
            
            loadThumbnail(channel)
            binding.tvChannelName.text = channel.name

            binding.btnWatchNow.setOnClickListener {
                onChannelClick(channel)
            }

            binding.root.setOnLongClickListener {
                onMoreClick(channel, it)
                true
            }
        }

        private fun loadThumbnail(channel: TVChannel) {
            showShimmer()
            
            val requestListener = object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean,
                ): Boolean {
                    hideShimmer()
                    setDefaultThumbnail()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean,
                ): Boolean {
                    hideShimmer()
                    return false
                }
            }

            val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .error(R.color.cast_channel_item_thumbnail_bg)
                .skipMemoryCache(false) // Enable memory cache

            when {
                channel.logo.isNotEmpty() && (channel.logo.startsWith("content://") || channel.logo.startsWith("file://")) -> {
                    try {
                        val uri = channel.logo.toUri()
                        loadVideoThumbnail(uri, requestOptions, requestListener)
                    } catch (e: Exception) {
                        hideShimmer()
                        setDefaultThumbnail()
                    }
                }

                channel.url.startsWith("content://") || channel.url.startsWith("file://") -> {
                    try {
                        val videoUri = channel.url.toUri()
                        loadVideoThumbnail(videoUri, requestOptions, requestListener)
                    } catch (e: Exception) {
                        hideShimmer()
                        setDefaultThumbnail()
                    }
                }
                channel.logo.isNotEmpty() -> {
                    Glide.with(binding.root.context)
                        .load(channel.logo)
                        .apply(requestOptions)
                        .listener(requestListener)
                        .into(binding.ivThumbnail)
                }
                else -> {
                    hideShimmer()
                    setDefaultThumbnail()
                }
            }
        }

        private fun showShimmer() {
            binding.shimmerThumbnail.visibility = View.VISIBLE
            if (!binding.shimmerThumbnail.isShimmerStarted) {
                binding.shimmerThumbnail.startShimmer()
            }
        }

        private fun hideShimmer() {
            if (binding.shimmerThumbnail.isShimmerStarted) {
                binding.shimmerThumbnail.stopShimmer()
            }
            binding.shimmerThumbnail.visibility = View.GONE
        }

        private fun loadVideoThumbnail(
            uri: Uri,
            requestOptions: RequestOptions,
            requestListener: RequestListener<Drawable>,
        ) {
            if (uri.scheme == "content") {
                if (canAccessUri(uri)) {
                    try {
                        Glide.with(binding.root.context)
                            .load(uri)
                            .apply(requestOptions)
                            .listener(requestListener)
                            .into(binding.ivThumbnail)
                    } catch (e: Exception) {
                        extractVideoThumbnail(uri, requestOptions, requestListener)
                    }
                } else {
                    extractVideoThumbnail(uri, requestOptions, requestListener)
                }
            } else {
                try {
                    Glide.with(binding.root.context)
                        .load(uri)
                        .apply(requestOptions)
                        .listener(requestListener)
                        .into(binding.ivThumbnail)
                } catch (e: Exception) {
                    extractVideoThumbnail(uri, requestOptions, requestListener)
                }
            }
        }

        private fun canAccessUri(uri: Uri): Boolean {
            return try {
                binding.root.context.contentResolver.openInputStream(uri)?.use {
                    true
                } ?: false
            } catch (e: SecurityException) {
                false
            } catch (e: Exception) {
                false
            }
        }

        private fun extractVideoThumbnail(
            uri: Uri,
            requestOptions: RequestOptions,
            requestListener: RequestListener<Drawable>,
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                var retriever: MediaMetadataRetriever? = null
                try {
                    retriever = MediaMetadataRetriever()
                    if (uri.scheme == "content") {
                        retriever.setDataSource(binding.root.context, uri)
                    } else {
                        retriever.setDataSource(uri.path ?: "")
                    }
                    val bitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    
                    withContext(Dispatchers.Main) {
                        if (bitmap != null) {
                            Glide.with(binding.root.context)
                                .load(bitmap)
                                .apply(requestOptions)
                                .listener(requestListener)
                                .into(binding.ivThumbnail)
                        } else {
                            hideShimmer()
                            setDefaultThumbnail()
                        }
                    }
                } catch (e: SecurityException) {
                    // Permission denied - persistent permission might not be granted
                    withContext(Dispatchers.Main) {
                        hideShimmer()
                        setDefaultThumbnail()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        hideShimmer()
                        setDefaultThumbnail()
                    }
                } finally {
                    retriever?.release()
                }
            }
        }

        private fun setDefaultThumbnail() {
            binding.ivThumbnail.setImageResource(R.drawable.cast_thumbnail_default)
        }
    }

    private class ChannelDiffCallback : DiffUtil.ItemCallback<TVChannel>() {
        override fun areItemsTheSame(oldItem: TVChannel, newItem: TVChannel): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: TVChannel, newItem: TVChannel): Boolean {
            return oldItem == newItem
        }
    }
}


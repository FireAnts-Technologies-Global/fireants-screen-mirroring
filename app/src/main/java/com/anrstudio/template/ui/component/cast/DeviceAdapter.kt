package com.anrstudio.template.ui.component.cast

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastItemDeviceBinding
import com.anrstudio.template.domain.model.CastDevice

class DeviceAdapter(
    private val onDeviceClick: (CastDevice) -> Unit,
    private val onDeviceLongClick: ((CastDevice) -> Unit)? = null,
    private val mediaPickerStyle: Boolean = false,
) : ListAdapter<CastDevice, DeviceAdapter.ViewHolder>(DiffCallback()) {

    var connectingId: String? = null
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        }

    var selectedId: String? = null
        set(value) {
            if (field != value) {
                field = value
                notifyItemRangeChanged(0, itemCount)
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CastItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), connectingId, selectedId)
    }

    inner class ViewHolder(private val binding: CastItemDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(device: CastDevice, connectingId: String?, selectedId: String?) {
            val isConnecting = device.id == connectingId
            val isSelected = device.isConnected || device.id == selectedId

            binding.tvDeviceName.text = device.name
            binding.tvModelName.text = device.modelName

            binding.rlCast.setBackgroundResource(
                when {
                    mediaPickerStyle && isSelected -> R.drawable.cast_bg_media_device_selected
                    mediaPickerStyle -> R.drawable.cast_bg_media_device_default
                    isSelected -> R.drawable.cast_bg_cast_device_selected
                    else -> R.drawable.cast_bg_cast_device
                }
            )

            if (mediaPickerStyle) {
                binding.rlCast.layoutParams = binding.rlCast.layoutParams.apply {
                    height = itemView.resources.getDimensionPixelSize(R.dimen.cast_media_device_row_height)
                }
                binding.flDeviceIcon.layoutParams = binding.flDeviceIcon.layoutParams.apply {
                    val iconTileSize = itemView.resources.getDimensionPixelSize(R.dimen.cast_media_device_icon_tile)
                    width = iconTileSize
                    height = iconTileSize
                }
                binding.imgCast.layoutParams = binding.imgCast.layoutParams.apply {
                    val iconSize = itemView.resources.getDimensionPixelSize(R.dimen.cast_media_device_icon)
                    width = iconSize
                    height = iconSize
                }
                binding.flDeviceIcon.setBackgroundResource(
                    if (isSelected) R.drawable.cast_bg_media_device_icon_selected
                    else R.drawable.cast_bg_media_device_icon_idle
                )
                binding.imgCast.setColorFilter(
                    ContextCompat.getColor(itemView.context, if (isSelected) android.R.color.white else R.color.cast_cast_secondary_text)
                )
                (binding.root.layoutParams as? RecyclerView.LayoutParams)?.let { params ->
                    params.bottomMargin = if (bindingAdapterPosition < itemCount - 1) {
                        itemView.resources.getDimensionPixelSize(R.dimen.cast_media_device_row_gap)
                    } else 0
                    binding.root.layoutParams = params
                }
            }

            if (isConnecting) {
                binding.imgIsConnect.visibility = View.INVISIBLE
                binding.progressConnecting.visibility = View.VISIBLE
            } else {
                binding.progressConnecting.visibility = View.GONE
                binding.imgIsConnect.visibility = View.VISIBLE
                binding.imgIsConnect.setImageResource(
                    if (device.isConnected) R.drawable.cast_ic_figma_check else R.drawable.cast_ic_figma_chevron
                )
            }

            binding.root.setOnClickListener { if (!isConnecting) onDeviceClick(device) }
            binding.root.setOnLongClickListener {
                if (!isConnecting) onDeviceLongClick?.invoke(device)
                onDeviceLongClick != null
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CastDevice>() {
        override fun areItemsTheSame(oldItem: CastDevice, newItem: CastDevice): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CastDevice, newItem: CastDevice): Boolean =
            oldItem == newItem
    }
}

package com.anrstudio.template.ui.component.cast

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anrstudio.template.R
import com.anrstudio.template.databinding.CastItemDeviceBinding
import com.anrstudio.template.domain.model.CastDevice

class DeviceAdapter(
    private val onDeviceClick: (CastDevice) -> Unit,
    private val onDeviceLongClick: ((CastDevice) -> Unit)? = null,
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
                if (isSelected) R.drawable.cast_bg_cast_device_item_selected
                else R.drawable.cast_bg_cast_device_item
            )

            if (isConnecting) {
                binding.imgIsConnect.visibility = View.INVISIBLE
                binding.progressConnecting.visibility = View.VISIBLE
            } else {
                binding.progressConnecting.visibility = View.GONE
                binding.imgIsConnect.visibility = View.VISIBLE
                binding.imgIsConnect.setImageResource(
                    if (device.isConnected) R.drawable.cast_ic_cast_tick else R.drawable.cast_ic_cast_arrow_right
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

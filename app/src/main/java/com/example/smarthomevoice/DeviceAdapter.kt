package com.example.smarthomevoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthomevoice.databinding.ItemDeviceBinding

class DeviceAdapter(
    private var devices: List<Device>
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    fun setData(newDevices: List<Device>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            binding.imgDeviceIcon.setImageResource(device.iconResId)
            binding.tvDeviceName.text = device.name

            // Set switch state based on device.isOn
            binding.switchToggle.isChecked = device.isOn

            // Toggle listener
            binding.switchToggle.setOnCheckedChangeListener { _, isChecked ->
                device.isOn = isChecked
                // No text to update, so just store the new state
            }
        }
    }
}

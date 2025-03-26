package com.example.smarthomevoice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthomevoice.databinding.ItemDeviceBinding

class DeviceAdapter(
    private var devices: List<Device>
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    // Cho phép cập nhật data nếu cần
    fun setData(newDevices: List<Device>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    // Tạo ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        // Inflate layout bằng View Binding
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    // Gán dữ liệu
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    // ViewHolder sử dụng binding
    inner class DeviceViewHolder(private val binding: ItemDeviceBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            // Thay thế itemView.imgDeviceIcon -> binding.imgDeviceIcon
            binding.imgDeviceIcon.setImageResource(device.iconResId)
            binding.tvDeviceName.text = device.name
            binding.tvDeviceStatus.text = device.status

            // Nếu cần bắt sự kiện click
            itemView.setOnClickListener {
                // ...
            }
        }
    }
}

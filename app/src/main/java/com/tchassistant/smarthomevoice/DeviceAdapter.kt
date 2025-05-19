package com.tchassistant.smarthomevoice

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.tchassistant.smarthomevoice.api.DeviceRepository
import com.tchassistant.smarthomevoice.databinding.ItemDeviceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceAdapter(
    private var devices: List<Device>
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val deviceRepository = DeviceRepository()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val TAG = "DeviceAdapter"

    fun getDevices(): List<Device> {
        return devices
    }


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
                coroutineScope.launch {
                    try {
                        Log.d(TAG, "Sending command for ${device.name}: $isChecked")
                        val result = when (device.name) {
                            "Fan" -> deviceRepository.controlFan(isChecked)
                            "Light" -> deviceRepository.controlRoomLight(isChecked)
                            "Curtain" -> deviceRepository.controlCurtain(if (isChecked) "open" else "close")
                            "Night Light" -> deviceRepository.controlReadingLamp(isChecked)
                            "Door" -> deviceRepository.controlDoor(if (isChecked) "open" else "close")
                            else -> null
                        }

                        result?.let {
                            it.onSuccess { response ->
                                Log.d(TAG, "Success: ${device.name} command sent successfully")
                                Toast.makeText(binding.root.context,
                                    "${device.name} ${if (isChecked) "turned on" else "turned off"}",
                                    Toast.LENGTH_SHORT).show()
                            }.onFailure { error ->
                                Log.e(TAG, "Error controlling ${device.name}: ${error.message}")
                                Toast.makeText(binding.root.context,
                                    "Error controlling ${device.name}: ${error.message}",
                                    Toast.LENGTH_SHORT).show()
                                // Revert the switch state on error
                                binding.switchToggle.isChecked = !isChecked
                                device.isOn = !isChecked
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception while controlling ${device.name}: ${e.message}")
                        Toast.makeText(binding.root.context,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                        // Revert the switch state on error
                        binding.switchToggle.isChecked = !isChecked
                        device.isOn = !isChecked
                    }
                }
            }
        }
    }
}
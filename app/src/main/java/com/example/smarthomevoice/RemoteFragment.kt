package com.example.smarthomevoice

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smarthomevoice.databinding.FragmentRemoteBinding

class RemoteFragment : Fragment() {

    private var _binding: FragmentRemoteBinding? = null
    private val binding get() = _binding!!
    private val TAG = "RemoteFragment"

    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var roomAdapter: ArrayAdapter<String>

    private val allDevices = listOf(
        Device("Fan", R.drawable.ic_fan, true, room = "Living Room"),
        Device("Light", R.drawable.ic_light, false, room = "Kitchen"),
        Device("Curtain", R.drawable.ic_curtain, true, room = "Living Room"),
        Device("Night Light", R.drawable.ic_night_light, false, room = "Bedroom"),
        Device("Door", R.drawable.ic_door, false, room = "Bedroom")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupRoomSpinner()

        // Configure the voice button in the fragment to use MainActivity's speech recognition
        binding.btnVoice.setOnClickListener {
            (activity as? MainActivity)?.startSpeechToText()
        }
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(allDevices)
        binding.recyclerViewDevices.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewDevices.adapter = deviceAdapter
    }

    private fun setupRoomSpinner() {
        val roomArray = resources.getStringArray(R.array.room_options)
        roomAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roomArray)
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRoom.adapter = roomAdapter

        binding.spinnerRoom.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedRoom = parent.getItemAtPosition(position) as String
                filterDevicesByRoom(selectedRoom)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                deviceAdapter.setData(allDevices)
            }
        }
    }

    /**
     * Select a room by name via voice command
     */
    fun selectRoom(roomName: String) {
        Log.d(TAG, "Selecting room: $roomName")
        val roomArray = resources.getStringArray(R.array.room_options)

        // Try to find an exact match first
        var position = roomArray.indexOfFirst { it.equals(roomName, ignoreCase = true) }

        // If no exact match, try partial match
        if (position == -1) {
            position = roomArray.indexOfFirst {
                it.contains(roomName, ignoreCase = true)
            }
        }

        // If room is found, select it in spinner
        if (position != -1) {
            binding.spinnerRoom.setSelection(position)
            showToast("Showing devices in $roomName")
        } else {
            // If not found, default to "All Rooms"
            binding.spinnerRoom.setSelection(0)
            showToast("Room '$roomName' not found. Showing all devices.")
        }
    }

    private fun filterDevicesByRoom(room: String) {
        if (room.equals("All Rooms", ignoreCase = true)) {
            deviceAdapter.setData(allDevices)
        } else {
            val filtered = allDevices.filter { it.room.equals(room, ignoreCase = true) }
            deviceAdapter.setData(filtered)
        }
    }

    /**
     * Update device state via voice command or UI interaction
     */
    fun updateDeviceState(deviceName: String, isOn: Boolean) {
        Log.d(TAG, "Updating device state: $deviceName to ${if(isOn) "ON" else "OFF"}")

        // First try to find device in currently displayed devices
        var deviceFound = false
        val currentDevices = deviceAdapter.getDevices()

        for (i in currentDevices.indices) {
            val device = currentDevices[i]
            if (device.name.equals(deviceName, ignoreCase = true)) {
                device.isOn = isOn
                deviceAdapter.notifyItemChanged(i)
                deviceFound = true

                // Create appropriate message based on device type
                val statusMsg = when {
                    device.name.equals("door", ignoreCase = true) -> if (isOn) "opened" else "closed"
                    device.name.equals("curtain", ignoreCase = true) -> if (isOn) "opened" else "closed"
                    else -> if (isOn) "turned on" else "turned off"
                }

                showToast("${device.name} $statusMsg")
                break
            }
        }

        // If not found in current view, search in all devices and update
        if (!deviceFound) {
            val allDeviceIndex = allDevices.indexOfFirst { it.name.equals(deviceName, ignoreCase = true) }
            if (allDeviceIndex != -1) {
                allDevices[allDeviceIndex].isOn = isOn

                // Find and select the room where this device is located
                val deviceRoom = allDevices[allDeviceIndex].room
                selectRoom(deviceRoom)

                // Create appropriate message based on device type
                val statusMsg = when {
                    deviceName.equals("door", ignoreCase = true) -> if (isOn) "opened" else "closed"
                    deviceName.equals("curtain", ignoreCase = true) -> if (isOn) "opened" else "closed"
                    else -> if (isOn) "turned on" else "turned off"
                }

                showToast("${deviceName} $statusMsg in $deviceRoom")
            } else {
                showToast("Device '$deviceName' not found")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
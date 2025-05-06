package com.example.smarthomevoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smarthomevoice.databinding.FragmentRemoteBinding

class RemoteFragment : Fragment() {

    private var _binding: FragmentRemoteBinding? = null
    private val binding get() = _binding!!

    private lateinit var deviceAdapter: DeviceAdapter

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
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(allDevices)
        binding.recyclerViewDevices.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerViewDevices.adapter = deviceAdapter
    }

    private fun setupRoomSpinner() {
        val roomArray = resources.getStringArray(R.array.room_options)
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roomArray)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRoom.adapter = spinnerAdapter

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

    private fun filterDevicesByRoom(room: String) {
        if (room.equals("All Rooms", ignoreCase = true)) {
            deviceAdapter.setData(allDevices)
        } else {
            val filtered = allDevices.filter { it.room.equals(room, ignoreCase = true) }
            deviceAdapter.setData(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

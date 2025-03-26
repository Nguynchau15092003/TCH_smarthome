package com.example.smarthomevoice

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.example.smarthomevoice.databinding.ActivityMainBinding // Import lớp binding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sử dụng View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tạo danh sách thiết bị
        val devices = listOf(
            Device("Fan", R.drawable.ic_fan, "Đang bật"),
            Device("Light", R.drawable.ic_light, "Đang tắt"),
            Device("Curtain", R.drawable.ic_curtain, "Đang mở")
        )

        // Khởi tạo Adapter
        deviceAdapter = DeviceAdapter(devices)

        // Setup RecyclerView
        binding.recyclerViewDevices.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewDevices.adapter = deviceAdapter

        // TabLayout
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { /* All devices */ }
                    1 -> { /* Living room */ }
                    2 -> { /* Kitchen */ }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // FAB (mic)
        binding.fabVoice.setOnClickListener {
            Toast.makeText(this, "Voice recognition triggered", Toast.LENGTH_SHORT).show()
        }
    }
}

package com.example.smarthomevoice

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.smarthomevoice.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Khởi tạo View Binding từ layout activity_main.xml
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tạo danh sách thiết bị (Device) với các thông tin mẫu
        val devices = listOf(
            Device("Fan", R.drawable.ic_fan, false),
            Device("Light", R.drawable.ic_light, false),
            Device("Curtain", R.drawable.ic_curtain, false),
            Device("Night Light", R.drawable.ic_night_light, false),
            Device("Door", R.drawable.ic_door, false)
        )

        // Khởi tạo adapter và thiết lập RecyclerView (hiển thị theo dạng lưới 2 cột)
        deviceAdapter = DeviceAdapter(devices)
        binding.recyclerViewDevices.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerViewDevices.adapter = deviceAdapter

        // Thiết lập TabLayout (nếu bạn muốn lọc theo tab, hiện tại dùng chung danh sách)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // TODO: Thêm logic lọc thiết bị theo tab nếu cần
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Xử lý sự kiện nhấn nút Voice (FloatingActionButton)
        binding.fabVoice.setOnClickListener {
            // TODO: Triển khai chức năng nhận dạng giọng nói
        }
    }
}

package com.example.smarthomevoice

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smarthomevoice.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gán topAppBar làm ActionBar
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.title = "TCH app"

        // Làm cho AppBar trong suốt và không bóng
        binding.topAppBar.setBackgroundColor(Color.TRANSPARENT)
        binding.topAppBar.elevation = 0f

        // Xử lý menu item click
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                    // Ví dụ chuyển sang màn hình đăng nhập
                    // startActivity(Intent(this, LoginActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Fragment mặc định
        replaceFragment(RemoteFragment())

        // Xử lý chuyển fragment qua bottom nav
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_remote -> {
                    replaceFragment(RemoteFragment())
                    true
                }
                R.id.nav_note -> {
                    replaceFragment(TakeNoteFragment())
                    true
                }
                R.id.nav_history -> {
                    replaceFragment(HistoryFragment())
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_app_bar_menu, menu)
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}

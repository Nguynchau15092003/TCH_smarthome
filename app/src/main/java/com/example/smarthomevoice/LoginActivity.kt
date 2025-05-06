package com.example.smarthomevoice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthomevoice.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            // Giả sử kiểm tra đăng nhập đơn giản (bạn thay thế bằng logic thực tế)
            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Đăng nhập thành công, hiển thị thông báo
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                // Chuyển sang MainActivity (màn hình All Devices)
                val intent = Intent(this, MainActivity::class.java)
                // Nếu cần truyền dữ liệu, có thể dùng intent.putExtra(...)
                startActivity(intent)
                finish() // Đóng LoginActivity
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

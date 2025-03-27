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
        // Sử dụng View Binding để inflate layout
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Xử lý logic đăng nhập (gọi API hoặc kiểm tra cục bộ)
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                // Sau khi đăng nhập thành công, chuyển sang MainActivity (All Devices)
                startActivity(Intent(this, MainActivity::class.java))
                finish() // Đóng LoginActivity
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

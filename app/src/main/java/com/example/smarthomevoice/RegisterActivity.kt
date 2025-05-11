package com.example.smarthomevoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.smarthomevoice.databinding.ActivityRegisterBinding

class RegisterActivity : ComponentActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Quay lại màn hình đăng nhập khi click vào "Sign In"
        binding.tvSignIn.setOnClickListener {
            finish()
        }

        // Chỉ thiết lập UI, không xử lý chức năng đăng ký
        binding.btnSignUp.setOnClickListener {
            // Để trống, chỉ làm UI
        }
    }
}
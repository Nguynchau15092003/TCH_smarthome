package com.example.smarthomevoice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.smarthomevoice.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : ComponentActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Navigate back to login screen when clicking "Sign In"
        binding.tvSignIn.setOnClickListener {
            finish()
        }

        // Handle sign up button click
        binding.btnSignUp.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        // Get user inputs
        val username = binding.etUsername.text.toString().trim()
        val mobile = binding.etMobile.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        val rememberMe = binding.switchRemember.isChecked

        // Validate inputs
        if (username.isEmpty() || mobile.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state (you can add a progress bar in the UI)
        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = "Creating Account..."

        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // User created successfully
                    val user = auth.currentUser

                    // Update profile with username
                    val profileUpdates = userProfileChangeRequest {
                        displayName = username
                    }

                    user?.updateProfile(profileUpdates)
                        ?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                // Store additional user data in Firestore
                                storeUserDataInFirestore(user.uid, username, email, mobile)
                            } else {
                                Toast.makeText(this, "Failed to update profile: ${profileTask.exception?.message}",
                                    Toast.LENGTH_SHORT).show()
                                resetSignUpButton()
                            }
                        }
                } else {
                    // If sign up fails, display a message to the user
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                    resetSignUpButton()
                }
            }
    }

    private fun storeUserDataInFirestore(userId: String, username: String, email: String, mobile: String) {
        val userData = hashMapOf(
            "username" to username,
            "email" to email,
            "mobile" to mobile,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()

                // Navigate to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity() // Close all activities in the stack
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to store user data: ${e.message}", Toast.LENGTH_SHORT).show()
                resetSignUpButton()
            }
    }

    private fun resetSignUpButton() {
        binding.btnSignUp.isEnabled = true
        binding.btnSignUp.text = "Sign Up"
    }
}
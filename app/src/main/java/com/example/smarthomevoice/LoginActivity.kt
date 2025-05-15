package com.example.smarthomevoice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.smarthomevoice.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.tasks.TasksScopes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class LoginActivity : ComponentActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Check if user is already signed in
        if (auth.currentUser != null) {
            // User is signed in, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Configure Google Sign-In with full read/write access to Calendar and Tasks
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(
                // Full access to Calendar (read, create, update, delete)
                Scope("https://www.googleapis.com/auth/calendar"),
                // Full access to Tasks (read, create, update, delete)
                Scope("https://www.googleapis.com/auth/tasks")
            )
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up regular login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading state
            setLoadingState(true)

            // Sign in with email and password
            loginWithEmailPassword(email, password)
        }

        // Set up Google Sign-In button
        binding.btnSignInWithGoogle.setOnClickListener {
            setLoadingState(true)
            signIn()
        }

        // Set up Forgot Password text
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        // Set up Sign Up text
        binding.tvSignUp.setOnClickListener {
            // Navigate to registration screen
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Set up the ActivityResultLauncher
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
        }
    }

    private fun loginWithEmailPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                }
            }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser

                    // Check if this is a new user
                    if (task.result?.additionalUserInfo?.isNewUser == true) {
                        // This is a new user, store their data in Firestore
                        user?.let {
                            val userData = hashMapOf(
                                "username" to user.displayName,
                                "email" to user.email,
                                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                "createdAt" to System.currentTimeMillis()
                            )

                            db.collection("users").document(user.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    navigateToMainActivity()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to store user data: ${e.message}",
                                        Toast.LENGTH_SHORT).show()
                                    navigateToMainActivity()
                                }
                        }
                    } else {
                        // Existing user, just navigate
                        navigateToMainActivity()
                    }
                } else {
                    // If sign in fails, display a message to the user
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                    setLoadingState(false)
                }
            }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showForgotPasswordDialog() {
        val email = binding.etEmail.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        setLoadingState(true)

        // Send password reset email
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                setLoadingState(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Signing in..."
            binding.btnSignInWithGoogle.isEnabled = false
        } else {
            binding.btnLogin.isEnabled = true
            binding.btnLogin.text = "Sign In"
            binding.btnSignInWithGoogle.isEnabled = true
        }
    }
}
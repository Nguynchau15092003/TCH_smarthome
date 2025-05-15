package com.tchassistant.smarthomevoice

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tchassistant.smarthomevoice.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = "MainActivity"

    // Speech recognition launcher
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                Log.d(TAG, "Recognized speech: $spokenText")
                // Voice command processing removed
            }
        } else {
            Toast.makeText(this, "Speech recognition failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Initializing app")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Request permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_RECORD_AUDIO
        )

        // Gán topAppBar làm ActionBar
        setSupportActionBar(binding.topAppBar)
        supportActionBar?.title = "TCH Assistant"

        // Làm cho AppBar trong suốt và không bóng
        binding.topAppBar.setBackgroundColor(Color.TRANSPARENT)
        binding.topAppBar.elevation = 0f

        // Voice button click listener
        binding.fabVoice.setOnClickListener {
            startSpeechToText()
        }

        // Xử lý menu item click
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    signOut()
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

    // Make this function public so it can be called from fragments if needed
    fun startSpeechToText() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command for your smart home")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            Toast.makeText(this, "Speech recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDeviceControlIntent(parameters: Map<String, com.google.protobuf.Value>) {
        val device = parameters["devicename"]?.stringValue?.lowercase()
        val status = parameters["devicestatus"]?.stringValue?.lowercase()

        Log.d(TAG, "Device control: Device=$device, Status=$status")

        if (!device.isNullOrEmpty() && !status.isNullOrEmpty()) {
            // Find the current RemoteFragment and update device state
            val remoteFragment = supportFragmentManager.fragments.find { it is RemoteFragment } as? RemoteFragment
            remoteFragment?.let { fragment ->
                when {
                    (device == "door" || device == "curtain") && status == "open" ->
                        fragment.updateDeviceState(device, true)
                    (device == "door" || device == "curtain") && status == "close" ->
                        fragment.updateDeviceState(device, false)
                    status == "on" -> fragment.updateDeviceState(device, true)
                    status == "off" -> fragment.updateDeviceState(device, false)
                    else -> showToast("Unknown status: $status for $device")
                }
            } ?: showToast("Remote control not available")
        } else {
            showToast("Device or status not recognized")
        }
    }

    private fun handleRoomSelectionIntent(parameters: Map<String, com.google.protobuf.Value>) {
        val room = parameters["room"]?.stringValue

        Log.d(TAG, "Room selection: Room=$room")

        if (!room.isNullOrEmpty()) {
            val remoteFragment = supportFragmentManager.fragments.find { it is RemoteFragment } as? RemoteFragment
            remoteFragment?.let { fragment ->
                fragment.selectRoom(room)
            } ?: showToast("Remote control not available")
        } else {
            showToast("Room not recognized")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun signOut() {
        // Sign out from Firebase
        auth.signOut()

        // Sign out from Google
        googleSignInClient.signOut().addOnCompleteListener(this) {
            // Navigate to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1001
    }
}
package com.example.smarthomevoice

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smarthomevoice.api.DialogflowService
import com.example.smarthomevoice.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dialogflowService: DialogflowService
    private val TAG = "MainActivity"

    // Speech recognition launcher
    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (!spokenText.isNullOrEmpty()) {
                Log.d(TAG, "Recognized speech: $spokenText")
                processVoiceCommand(spokenText)
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

        // Initialize Dialogflow service
        dialogflowService = DialogflowService(this)

        // Request permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_RECORD_AUDIO
        )

        // Voice button click listener
        binding.fabVoice.setOnClickListener {
            startSpeechToText()
        }

        // Make sure the FAB has the mic icon
        binding.fabVoice.setImageResource(R.drawable.ic_mic)

        // Set up Bottom Navigation to switch between fragments
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
                R.id.nav_logout -> {
                    signOut()
                    true
                }
                else -> false
            }
        }

        // Display RemoteFragment by default
        replaceFragment(RemoteFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Make this function public so it can be called from fragments
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

    private fun processVoiceCommand(command: String) {
        Log.d(TAG, "Processing voice command: $command")
        binding.fabVoice.isEnabled = false // Disable button while processing

        lifecycleScope.launch {
            try {
                val response = dialogflowService.detectIntent(command)
                val intentName = response.queryResult.intent.displayName
                val parameters = response.queryResult.parameters.fieldsMap
                val fulfillmentText = response.queryResult.fulfillmentText

                Log.d(TAG, "Dialogflow Intent: $intentName, Parameters: $parameters")
                Log.d(TAG, "Fulfillment text: $fulfillmentText")

                when (intentName) {
                    "devicecontrol" -> handleDeviceControlIntent(parameters)
                    "roomselection" -> handleRoomSelectionIntent(parameters)
                    else -> showToast("I didn't understand that command")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice command", e)
                showToast("Error: ${e.message}")
            } finally {
                binding.fabVoice.isEnabled = true // Re-enable button after processing
            }
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
        FirebaseAuth.getInstance().signOut()
        
        // Sign out from Google
        GoogleSignIn.getClient(this, GoogleSignIn.getLastSignedInAccount(this)?.account?.let {
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        } ?: com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        ).signOut().addOnCompleteListener {
            // Navigate back to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogflowService.shutdown()
    }

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 1001
    }
}